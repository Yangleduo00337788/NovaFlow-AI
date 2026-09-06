package ai.novaflow.user.service;

import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.tenant.entity.TenantEntity;
import ai.novaflow.user.domain.vo.PlatformSecurityOverviewVO;
import ai.novaflow.user.entity.PlatformSecurityAlertEventEntity;
import ai.novaflow.user.entity.UserEntity;
import ai.novaflow.user.mapper.PlatformSecurityAlertEventMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PlatformRiskControlService {

    public static final String ALERT_ABNORMAL_LOGIN = "ABNORMAL_LOGIN";
    public static final String ALERT_BATCH_REGISTER = "BATCH_REGISTER";
    public static final String ALERT_NEW_USER_AGENT = "NEW_USER_AGENT";
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_ACKED = "ACKED";

    private final PlatformSystemConfigService platformSystemConfigService;
    private final PlatformSecurityAlertEventMapper securityAlertEventMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public void checkBatchRegisterAllowed(String clientIp) {
        int limit = platformSystemConfigService.getBatchRegisterIpLimitPerDay();
        if (limit <= 0) {
            return;
        }
        String ip = normalizeIp(clientIp);
        String key = "novaflow:risk:register:ip:" + ip + ":" + LocalDate.now();
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, Duration.ofDays(1));
        }
        if (count != null && count > limit) {
            persistAlert(
                    ALERT_BATCH_REGISTER,
                    "critical",
                    null,
                    null,
                    null,
                    ip,
                    null,
                    "同 IP 批量注册超限: " + ip + " (" + count + "/" + limit + ")",
                    count,
                    (long) limit,
                    "ip:" + ip);
            throw new BusinessException("当前 IP 注册次数过多，请稍后再试或联系平台管理员");
        }
    }

    public void onRegisterSuccess(String email, String clientIp, String userAgent, Long tenantId) {
        String ip = normalizeIp(clientIp);
        long count = parseLong(stringRedisTemplate.opsForValue().get(
                "novaflow:risk:register:ip:" + ip + ":" + LocalDate.now()), 0L);
        int limit = platformSystemConfigService.getBatchRegisterIpLimitPerDay();
        if (limit > 0 && count >= Math.max(1, limit - 1)) {
            persistAlert(
                    ALERT_BATCH_REGISTER,
                    count >= limit ? "critical" : "warning",
                    null,
                    email,
                    tenantId,
                    ip,
                    truncateUserAgent(userAgent),
                    "注册成功但 IP 注册频繁: " + ip + " (" + count + "/" + limit + ")",
                    count,
                    (long) limit,
                    "ip:" + ip + ":warn");
        }
    }

    public void onTenantLoginSuccess(
            UserEntity user,
            TenantEntity tenant,
            String clientIp,
            String userAgent,
            String previousIp) {
        String ip = normalizeIp(clientIp);
        String agent = truncateUserAgent(userAgent);

        if (platformSystemConfigService.isAbnormalLoginEnabled()
                && StringUtils.hasText(previousIp)
                && !previousIp.equals(ip)) {
            persistAlert(
                    ALERT_ABNORMAL_LOGIN,
                    "warning",
                    user.getId(),
                    user.getEmail(),
                    tenant.getId(),
                    ip,
                    agent,
                    "异常登录 IP: " + user.getEmail() + " 上次 " + previousIp + " → 本次 " + ip,
                    null,
                    null,
                    "user:" + user.getId() + ":ip:" + ip);
        }

        if (platformSystemConfigService.isNewUserAgentEnabled()
                && StringUtils.hasText(agent)) {
            String agentKey = "novaflow:risk:ua:" + user.getId();
            String previousAgent = stringRedisTemplate.opsForValue().get(agentKey);
            if (StringUtils.hasText(previousAgent) && !previousAgent.equals(agent)) {
                persistAlert(
                        ALERT_NEW_USER_AGENT,
                        "warning",
                        user.getId(),
                        user.getEmail(),
                        tenant.getId(),
                        ip,
                        agent,
                        "新设备/浏览器登录: " + user.getEmail(),
                        null,
                        null,
                        "user:" + user.getId() + ":ua:" + Integer.toHexString(agent.hashCode()));
            }
            stringRedisTemplate.opsForValue().set(agentKey, agent, Duration.ofDays(90));
        }
    }

    public PlatformSecurityOverviewVO securityOverview() {
        long abnormal = countOpen(ALERT_ABNORMAL_LOGIN);
        long batch = countOpen(ALERT_BATCH_REGISTER);
        long ua = countOpen(ALERT_NEW_USER_AGENT);
        return PlatformSecurityOverviewVO.builder()
                .openAlertCount(abnormal + batch + ua)
                .abnormalLoginOpenCount(abnormal)
                .batchRegisterOpenCount(batch)
                .newUserAgentOpenCount(ua)
                .build();
    }

    private long countOpen(String alertType) {
        return securityAlertEventMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq("alert_type", alertType)
                        .eq("status", STATUS_OPEN));
    }

    private void persistAlert(
            String alertType,
            String severity,
            Long userId,
            String userEmail,
            Long tenantId,
            String clientIp,
            String userAgent,
            String message,
            Long metricValue,
            Long threshold,
            String dedupeKey) {
        LocalDate today = LocalDate.now();
        PlatformSecurityAlertEventEntity existing = securityAlertEventMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("alert_type", alertType)
                        .eq("dedupe_key", dedupeKey)
                        .eq("event_date", today));
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            if (STATUS_ACKED.equals(existing.getStatus())) {
                return;
            }
            existing.setSeverity(severity);
            existing.setMessage(message);
            existing.setMetricValue(metricValue);
            existing.setThreshold(threshold);
            existing.setClientIp(clientIp);
            existing.setUserAgent(userAgent);
            existing.setUpdatedAt(now);
            securityAlertEventMapper.update(existing);
            return;
        }
        PlatformSecurityAlertEventEntity entity = new PlatformSecurityAlertEventEntity();
        entity.setAlertType(alertType);
        entity.setSeverity(severity);
        entity.setUserId(userId);
        entity.setUserEmail(userEmail);
        entity.setTenantId(tenantId);
        entity.setClientIp(clientIp);
        entity.setUserAgent(userAgent);
        entity.setMessage(message);
        entity.setMetricValue(metricValue);
        entity.setThreshold(threshold);
        entity.setStatus(STATUS_OPEN);
        entity.setEventDate(today);
        entity.setDedupeKey(dedupeKey);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        securityAlertEventMapper.insert(entity);
    }

    private String normalizeIp(String clientIp) {
        return StringUtils.hasText(clientIp) ? clientIp.trim() : "unknown";
    }

    private String truncateUserAgent(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return null;
        }
        String trimmed = userAgent.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }

    private long parseLong(String raw, long defaultValue) {
        if (!StringUtils.hasText(raw)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public static String resolveAlertTypeLabel(String alertType) {
        if (alertType == null) {
            return "";
        }
        return switch (alertType.toUpperCase(Locale.ROOT)) {
            case ALERT_ABNORMAL_LOGIN -> "异常登录 IP";
            case ALERT_BATCH_REGISTER -> "批量注册";
            case ALERT_NEW_USER_AGENT -> "新设备登录";
            default -> alertType;
        };
    }
}
