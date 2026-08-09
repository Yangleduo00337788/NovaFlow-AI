package ai.novaflow.billing.service;

import ai.novaflow.billing.domain.dto.BillingAlertSaveRequest;
import ai.novaflow.billing.domain.vo.BillingAlertVO;
import ai.novaflow.billing.entity.BillingAlertEntity;
import ai.novaflow.billing.mapper.BillingAlertMapper;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.model.mapper.TokenUsageMapper;
import ai.novaflow.user.entity.TenantEntity;
import ai.novaflow.user.mapper.TenantMapper;
import ai.novaflow.user.service.NotificationService;
import ai.novaflow.user.service.PermissionService;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BillingAlertService {

    private static final String ALERT_TYPE_TOKEN_QUOTA = "token_quota";

    private final BillingAlertMapper billingAlertMapper;
    private final TenantMapper tenantMapper;
    private final TokenUsageMapper tokenUsageMapper;
    private final NotificationService notificationService;
    private final PermissionService permissionService;

    public List<BillingAlertVO> listAlerts(Long tenantId) {
        ensureDefaultAlerts(tenantId);
        return billingAlertMapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("tenant_id", tenantId)
                                .orderBy("threshold_percent", true))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional
    public BillingAlertVO saveAlert(Long tenantId, Long userId, BillingAlertSaveRequest request) {
        requireBillingManagePermission(userId, tenantId);
        BillingAlertEntity entity;
        if (request.getId() != null) {
            entity = billingAlertMapper.selectOneByQuery(
                    QueryWrapper.create()
                            .eq("id", request.getId())
                            .eq("tenant_id", tenantId)
            );
            if (entity == null) {
                throw new BusinessException("预警配置不存在");
            }
        } else {
            entity = new BillingAlertEntity();
            entity.setTenantId(tenantId);
            entity.setAlertType(ALERT_TYPE_TOKEN_QUOTA);
            entity.setCreatedBy(userId);
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setAlertName(request.getAlertName().trim());
        entity.setThresholdPercent(request.getThresholdPercent());
        entity.setIsEnabled(Boolean.TRUE.equals(request.getEnabled()) ? 1 : 0);
        entity.setNotifyChannels(joinChannels(request.getNotifyChannels()));
        entity.setUpdatedAt(LocalDateTime.now());

        if (entity.getId() == null) {
            billingAlertMapper.insert(entity);
        } else {
            billingAlertMapper.update(entity);
        }
        return toVO(entity);
    }

    @Transactional
    public void checkAlerts(Long tenantId) {
        if (tenantId == null) {
            return;
        }
        ensureDefaultAlerts(tenantId);
        TenantEntity tenant = tenantMapper.selectOneById(tenantId);
        if (tenant == null || tenant.getMonthlyTokenQuota() == null || tenant.getMonthlyTokenQuota() <= 0) {
            return;
        }

        YearMonth current = YearMonth.now();
        LocalDate start = current.atDay(1);
        LocalDate end = current.atEndOfMonth();
        long usedTokens = safeLong(tokenUsageMapper.sumTokensBetween(tenantId, start, end));
        long quota = tenant.getMonthlyTokenQuota();
        int usedPercent = (int) Math.min(100, Math.round(usedTokens * 100.0 / quota));

        List<BillingAlertEntity> alerts = billingAlertMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("alert_type", ALERT_TYPE_TOKEN_QUOTA)
                        .eq("is_enabled", 1)
                        .orderBy("threshold_percent", true)
        );

        LocalDateTime monthStart = start.atStartOfDay();
        for (BillingAlertEntity alert : alerts) {
            if (alert.getThresholdPercent() == null || usedPercent < alert.getThresholdPercent()) {
                continue;
            }
            if (alert.getLastTriggeredAt() != null && !alert.getLastTriggeredAt().isBefore(monthStart)) {
                continue;
            }
            triggerAlert(tenantId, alert, usedTokens, quota, usedPercent);
        }
    }

    private void triggerAlert(
            Long tenantId,
            BillingAlertEntity alert,
            long usedTokens,
            long quota,
            int usedPercent) {
        String title = String.format(Locale.CHINA, "Token 用量已达 %d%%", alert.getThresholdPercent());
        String content = String.format(
                Locale.CHINA,
                "本月 Token 已使用 %,d / %,d（%d%%），已达到预警阈值 %d%%。",
                usedTokens,
                quota,
                usedPercent,
                alert.getThresholdPercent()
        );
        List<String> channels = parseChannels(alert.getNotifyChannels());
        if (channels.contains("site")) {
            notificationService.notifyTenantAdmins(tenantId, "billing", title, content, "/billing");
        }
        alert.setLastTriggeredAt(LocalDateTime.now());
        alert.setUpdatedAt(LocalDateTime.now());
        billingAlertMapper.update(alert);
    }

    private void ensureDefaultAlerts(Long tenantId) {
        long count = billingAlertMapper.selectCountByQuery(
                QueryWrapper.create().eq("tenant_id", tenantId)
        );
        if (count > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        createDefaultAlert(tenantId, "Token 用量 80% 预警", 80, now);
        createDefaultAlert(tenantId, "Token 用量 100% 预警", 100, now);
    }

    private void createDefaultAlert(Long tenantId, String name, int threshold, LocalDateTime now) {
        BillingAlertEntity entity = new BillingAlertEntity();
        entity.setTenantId(tenantId);
        entity.setAlertName(name);
        entity.setAlertType(ALERT_TYPE_TOKEN_QUOTA);
        entity.setThresholdPercent(threshold);
        entity.setNotifyChannels("site");
        entity.setIsEnabled(1);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        billingAlertMapper.insert(entity);
    }

    private BillingAlertVO toVO(BillingAlertEntity entity) {
        return BillingAlertVO.builder()
                .id(entity.getId())
                .alertName(entity.getAlertName())
                .alertType(entity.getAlertType())
                .thresholdPercent(entity.getThresholdPercent())
                .enabled(entity.getIsEnabled() != null && entity.getIsEnabled() == 1)
                .notifyChannels(parseChannels(entity.getNotifyChannels()))
                .lastTriggeredAt(entity.getLastTriggeredAt())
                .build();
    }

    private List<String> parseChannels(String channels) {
        if (!StringUtils.hasText(channels)) {
            return List.of("site");
        }
        return Arrays.stream(channels.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String joinChannels(List<String> channels) {
        if (channels == null || channels.isEmpty()) {
            return "site";
        }
        List<String> normalized = new ArrayList<>();
        for (String channel : channels) {
            if (StringUtils.hasText(channel) && !normalized.contains(channel.trim())) {
                normalized.add(channel.trim());
            }
        }
        return normalized.isEmpty() ? "site" : String.join(",", normalized);
    }

    private long safeLong(Long value) {
        return value != null ? value : 0L;
    }

    private void requireBillingManagePermission(long userId, Long tenantId) {
        permissionService.requireAnyPermission(userId, tenantId, "billing:manage");
    }
}
