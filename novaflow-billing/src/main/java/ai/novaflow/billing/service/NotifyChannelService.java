package ai.novaflow.billing.service;
import ai.novaflow.common.security.PermissionCodes;

import ai.novaflow.billing.domain.dto.NotifyChannelSaveRequest;
import ai.novaflow.billing.domain.vo.NotifyChannelVO;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.util.PublicHttpUrls;
import ai.novaflow.tenant.entity.TenantNotifyChannelEntity;
import ai.novaflow.tenant.mapper.TenantNotifyChannelMapper;
import ai.novaflow.user.service.PermissionService;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class NotifyChannelService {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final TenantNotifyChannelMapper tenantNotifyChannelMapper;
    private final PermissionService permissionService;
    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    public NotifyChannelVO get(Long tenantId) {
        requireManage(tenantId);
        return toVO(loadOrNull(tenantId));
    }

    @Transactional
    public NotifyChannelVO save(Long tenantId, NotifyChannelSaveRequest request) {
        requireManage(tenantId);
        TenantNotifyChannelEntity entity = loadOrNull(tenantId);
        LocalDateTime now = LocalDateTime.now();
        if (entity == null) {
            entity = new TenantNotifyChannelEntity();
            entity.setTenantId(tenantId);
            entity.setCreatedAt(now);
        }
        boolean webhookEnabled = Boolean.TRUE.equals(request.getWebhookEnabled());
        String webhookUrl = trimToNull(request.getWebhookUrl());
        if (webhookEnabled) {
            if (!PublicHttpUrls.isSafeWebhookUrl(webhookUrl)) {
                throw new BusinessException("Webhook 地址无效，仅支持公网 http(s) 地址");
            }
        }
        String emails = normalizeRecipients(request.getEmailRecipients());
        entity.setEmailEnabled(Boolean.TRUE.equals(request.getEmailEnabled()) ? 1 : 0);
        entity.setEmailRecipients(emails);
        entity.setWebhookEnabled(webhookEnabled ? 1 : 0);
        entity.setWebhookUrl(webhookUrl);
        if (StringUtils.hasText(request.getWebhookSecret())) {
            entity.setWebhookSecret(request.getWebhookSecret().trim());
        }
        entity.setUpdatedAt(now);
        if (entity.getId() == null) {
            tenantNotifyChannelMapper.insert(entity);
        } else {
            tenantNotifyChannelMapper.update(entity);
        }
        return toVO(entity);
    }

    TenantNotifyChannelEntity loadOrNull(Long tenantId) {
        return tenantNotifyChannelMapper.selectOneByQuery(
                QueryWrapper.create().eq("tenant_id", tenantId).limit(1));
    }

    private NotifyChannelVO toVO(TenantNotifyChannelEntity entity) {
        return NotifyChannelVO.builder()
                .emailEnabled(entity != null && entity.getEmailEnabled() != null && entity.getEmailEnabled() == 1)
                .emailRecipients(entity != null ? entity.getEmailRecipients() : null)
                .webhookEnabled(entity != null && entity.getWebhookEnabled() != null && entity.getWebhookEnabled() == 1)
                .webhookUrl(entity != null ? entity.getWebhookUrl() : null)
                .webhookSecretSet(entity != null && StringUtils.hasText(entity.getWebhookSecret()))
                .mailConfigured(mailSender.getIfAvailable() != null && StringUtils.hasText(mailHost))
                .build();
    }

    static String normalizeRecipients(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        List<String> emails = Arrays.stream(raw.split("[,;\\s]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(item -> item.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
        if (emails.size() > 10) {
            throw new BusinessException("收件人不能超过 10 个");
        }
        for (String email : emails) {
            if (!EMAIL.matcher(email).matches()) {
                throw new BusinessException("邮箱格式无效: " + email);
            }
        }
        return String.join(",", emails);
    }

    private void requireManage(Long tenantId) {
        permissionService.requireAnyPermission(StpUtil.getLoginIdAsLong(), tenantId, PermissionCodes.BILLING_MANAGE);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
