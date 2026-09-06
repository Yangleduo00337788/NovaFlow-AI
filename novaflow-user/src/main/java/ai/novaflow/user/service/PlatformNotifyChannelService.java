package ai.novaflow.user.service;

import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.util.PublicHttpUrls;
import ai.novaflow.user.domain.dto.PlatformNotifyChannelSaveRequest;
import ai.novaflow.user.domain.vo.PlatformNotifyChannelVO;
import ai.novaflow.user.entity.PlatformNotifyChannelEntity;
import ai.novaflow.user.mapper.PlatformNotifyChannelMapper;
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
public class PlatformNotifyChannelService {

    private static final long SINGLETON_ID = 1L;
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final PlatformNotifyChannelMapper platformNotifyChannelMapper;
    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    public PlatformNotifyChannelVO get() {
        return toVO(loadSingleton());
    }

    @Transactional
    public PlatformNotifyChannelVO save(PlatformNotifyChannelSaveRequest request) {
        PlatformNotifyChannelEntity entity = loadSingleton();
        LocalDateTime now = LocalDateTime.now();
        boolean webhookEnabled = Boolean.TRUE.equals(request.getWebhookEnabled());
        String webhookUrl = trimToNull(request.getWebhookUrl());
        if (webhookEnabled && !PublicHttpUrls.isSafeWebhookUrl(webhookUrl)) {
            throw new BusinessException("Webhook 地址无效，仅支持公网 http(s) 地址");
        }
        entity.setEmailEnabled(Boolean.TRUE.equals(request.getEmailEnabled()) ? 1 : 0);
        entity.setEmailRecipients(normalizeRecipients(request.getEmailRecipients()));
        entity.setWebhookEnabled(webhookEnabled ? 1 : 0);
        entity.setWebhookUrl(webhookUrl);
        if (StringUtils.hasText(request.getWebhookSecret())) {
            entity.setWebhookSecret(request.getWebhookSecret().trim());
        }
        entity.setUpdatedAt(now);
        platformNotifyChannelMapper.update(entity);
        return toVO(entity);
    }

    PlatformNotifyChannelEntity loadSingleton() {
        PlatformNotifyChannelEntity entity = platformNotifyChannelMapper.selectOneById(SINGLETON_ID);
        if (entity != null) {
            return entity;
        }
        LocalDateTime now = LocalDateTime.now();
        entity = new PlatformNotifyChannelEntity();
        entity.setId(SINGLETON_ID);
        entity.setEmailEnabled(0);
        entity.setWebhookEnabled(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        platformNotifyChannelMapper.insert(entity);
        return entity;
    }

    private PlatformNotifyChannelVO toVO(PlatformNotifyChannelEntity entity) {
        return PlatformNotifyChannelVO.builder()
                .emailEnabled(entity.getEmailEnabled() != null && entity.getEmailEnabled() == 1)
                .emailRecipients(entity.getEmailRecipients())
                .webhookEnabled(entity.getWebhookEnabled() != null && entity.getWebhookEnabled() == 1)
                .webhookUrl(entity.getWebhookUrl())
                .webhookSecretSet(StringUtils.hasText(entity.getWebhookSecret()))
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
