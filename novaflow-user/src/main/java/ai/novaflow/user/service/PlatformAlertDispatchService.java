package ai.novaflow.user.service;

import ai.novaflow.common.security.AccountTypes;
import ai.novaflow.common.util.PublicHttpUrls;
import ai.novaflow.user.entity.PlatformApiAlertEventEntity;
import ai.novaflow.user.entity.PlatformNotifyChannelEntity;
import ai.novaflow.user.entity.PlatformSecurityAlertEventEntity;
import ai.novaflow.user.entity.UserEntity;
import ai.novaflow.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformAlertDispatchService {

    public static final String CATEGORY_SECURITY = "security";
    public static final String CATEGORY_API_MONITOR = "api_monitor";
    public static final String CATEGORY_STORAGE_QUOTA = "storage_quota";

    private final PlatformNotifyChannelService platformNotifyChannelService;
    private final PlatformSystemConfigService platformSystemConfigService;
    private final UserMapper userMapper;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Value("${novaflow.alert.mail-from:}")
    private String mailFrom;

    @Value("${spring.mail.host:}")
    private String mailHost;

    public void dispatchSecurityAlert(PlatformSecurityAlertEventEntity alert) {
        dispatch(
                CATEGORY_SECURITY,
                platformSystemConfigService.getSecurityAlertChannels(),
                "platform.security." + alert.getAlertType().toLowerCase(),
                buildSecurityTitle(alert),
                alert.getMessage(),
                alertPayload(alert));
    }

    public void dispatchApiMonitorAlert(PlatformApiAlertEventEntity alert) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("alertId", alert.getId());
        payload.put("alertType", alert.getAlertType());
        payload.put("severity", alert.getSeverity());
        payload.put("tenantId", alert.getTenantId());
        payload.put("tenantName", alert.getTenantName());
        payload.put("metricValue", alert.getMetricValue());
        payload.put("threshold", alert.getThreshold());
        dispatch(
                CATEGORY_API_MONITOR,
                platformSystemConfigService.getApiMonitorAlertChannels(),
                "platform.api_monitor." + alert.getAlertType().toLowerCase(),
                "[API 监控] " + alert.getMessage(),
                alert.getMessage(),
                payload);
    }

    public void dispatchStorageQuotaAlert(
            Long tenantId,
            String tenantName,
            int usedPercent,
            int warnPercent,
            long usedBytes,
            int maxStorageMb,
            String severity) {
        String message = String.format(
                "租户 %s 存储使用率 %d%%（阈值 %d%%，已用 %d MB / 上限 %d MB）",
                tenantName,
                usedPercent,
                warnPercent,
                usedBytes / (1024L * 1024L),
                maxStorageMb);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", tenantId);
        payload.put("tenantName", tenantName);
        payload.put("usedPercent", usedPercent);
        payload.put("warnPercent", warnPercent);
        payload.put("usedBytes", usedBytes);
        payload.put("maxStorageMb", maxStorageMb);
        payload.put("severity", severity);
        dispatch(
                CATEGORY_STORAGE_QUOTA,
                platformSystemConfigService.getStorageQuotaAlertChannels(),
                "platform.storage_quota." + severity,
                "[存储配额] " + message,
                message,
                payload);
    }

    public void sendTestNotification() {
        dispatch(
                CATEGORY_SECURITY,
                List.of("email", "webhook"),
                "platform.notify.test",
                "[NovaFlow] 平台告警通道测试",
                "这是一条平台外部告警测试消息，用于验证邮件与 Webhook 配置。",
                Map.of("test", true));
    }

    private void dispatch(
            String category,
            List<String> channels,
            String event,
            String title,
            String content,
            Map<String, Object> extra) {
        if (channels == null || channels.isEmpty()) {
            return;
        }
        if (channels.contains("email")) {
            sendEmail(title, content);
        }
        if (channels.contains("webhook")) {
            sendWebhook(event, title, content, extra);
        }
    }

    private void sendEmail(String title, String content) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || !StringUtils.hasText(mailHost)) {
            log.info("Skip platform alert email: mail sender not configured");
            return;
        }
        PlatformNotifyChannelEntity channel = platformNotifyChannelService.loadSingleton();
        if (channel.getEmailEnabled() == null || channel.getEmailEnabled() != 1) {
            log.info("Skip platform alert email: channel disabled");
            return;
        }
        List<String> recipients = resolveRecipients(channel);
        if (recipients.isEmpty()) {
            log.warn("Skip platform alert email: no recipients");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (StringUtils.hasText(mailFrom)) {
                message.setFrom(mailFrom.trim());
            }
            message.setTo(recipients.toArray(String[]::new));
            message.setSubject("[NovaFlow] " + title);
            message.setText(content);
            sender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send platform alert email", e);
        }
    }

    private void sendWebhook(String event, String title, String content, Map<String, Object> extra) {
        PlatformNotifyChannelEntity channel = platformNotifyChannelService.loadSingleton();
        if (channel.getWebhookEnabled() == null || channel.getWebhookEnabled() != 1) {
            log.info("Skip platform alert webhook: channel disabled");
            return;
        }
        String url = channel.getWebhookUrl();
        if (!PublicHttpUrls.isSafeWebhookUrl(url)) {
            log.warn("Skip platform alert webhook: unsafe url");
            return;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("event", event);
            payload.put("title", title);
            payload.put("content", content);
            payload.put("occurredAt", LocalDateTime.now().toString());
            if (extra != null) {
                payload.putAll(extra);
            }
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url.trim()))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            if (StringUtils.hasText(channel.getWebhookSecret())) {
                request.header("X-NovaFlow-Signature", "sha256=" + hmacSha256(channel.getWebhookSecret(), body));
            }
            HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.warn("Platform alert webhook returned {}", response.statusCode());
            }
        } catch (Exception e) {
            log.warn("Failed to send platform alert webhook", e);
        }
    }

    private List<String> resolveRecipients(PlatformNotifyChannelEntity channel) {
        if (StringUtils.hasText(channel.getEmailRecipients())) {
            return List.of(channel.getEmailRecipients().split(","));
        }
        return userMapper.selectListByQuery(
                        QueryWrapper.create()
                                .eq("account_type", AccountTypes.PLATFORM)
                                .eq("is_deleted", 0)
                                .eq("status", 1))
                .stream()
                .map(UserEntity::getEmail)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String buildSecurityTitle(PlatformSecurityAlertEventEntity alert) {
        return "[安全风控] " + PlatformRiskControlService.resolveAlertTypeLabel(alert.getAlertType());
    }

    private Map<String, Object> alertPayload(PlatformSecurityAlertEventEntity alert) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("alertId", alert.getId());
        payload.put("alertType", alert.getAlertType());
        payload.put("severity", alert.getSeverity());
        payload.put("userId", alert.getUserId());
        payload.put("userEmail", alert.getUserEmail());
        payload.put("tenantId", alert.getTenantId());
        payload.put("clientIp", alert.getClientIp());
        payload.put("metricValue", alert.getMetricValue());
        payload.put("threshold", alert.getThreshold());
        return payload;
    }

    static String hmacSha256(String secret, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
