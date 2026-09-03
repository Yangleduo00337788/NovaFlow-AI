package ai.novaflow.billing.service;

import ai.novaflow.billing.entity.BillingAlertEntity;
import ai.novaflow.common.util.PublicHttpUrls;
import ai.novaflow.tenant.entity.TenantEntity;
import ai.novaflow.tenant.entity.TenantNotifyChannelEntity;
import ai.novaflow.tenant.mapper.TenantMapper;
import ai.novaflow.user.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class AlertDispatchService {

    private final TenantMapper tenantMapper;
    private final NotifyChannelService notifyChannelService;
    private final NotificationService notificationService;
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

    public void dispatch(
            Long tenantId,
            BillingAlertEntity alert,
            List<String> channels,
            String title,
            String content,
            long usedTokens,
            long quota,
            int usedPercent) {
        if (channels.contains("email")) {
            sendEmail(tenantId, title, content);
        }
        if (channels.contains("webhook")) {
            sendWebhook(tenantId, alert, title, content, usedTokens, quota, usedPercent);
        }
    }

    private void sendEmail(Long tenantId, String title, String content) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || !StringUtils.hasText(mailHost)) {
            log.info("Skip billing alert email: mail sender not configured, tenantId={}", tenantId);
            return;
        }
        TenantNotifyChannelEntity channel = notifyChannelService.loadOrNull(tenantId);
        if (channel == null || channel.getEmailEnabled() == null || channel.getEmailEnabled() != 1) {
            log.info("Skip billing alert email: channel disabled, tenantId={}", tenantId);
            return;
        }
        List<String> recipients = resolveRecipients(tenantId, channel);
        if (recipients.isEmpty()) {
            log.warn("Skip billing alert email: no recipients, tenantId={}", tenantId);
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
            log.warn("Failed to send billing alert email, tenantId={}", tenantId, e);
        }
    }

    private List<String> resolveRecipients(Long tenantId, TenantNotifyChannelEntity channel) {
        if (StringUtils.hasText(channel.getEmailRecipients())) {
            return List.of(channel.getEmailRecipients().split(","));
        }
        TenantEntity tenant = tenantMapper.selectOneById(tenantId);
        if (tenant != null && StringUtils.hasText(tenant.getContactEmail())) {
            return List.of(tenant.getContactEmail().trim());
        }
        return notificationService.listTenantAdminEmails(tenantId);
    }

    private void sendWebhook(
            Long tenantId,
            BillingAlertEntity alert,
            String title,
            String content,
            long usedTokens,
            long quota,
            int usedPercent) {
        TenantNotifyChannelEntity channel = notifyChannelService.loadOrNull(tenantId);
        if (channel == null || channel.getWebhookEnabled() == null || channel.getWebhookEnabled() != 1) {
            log.info("Skip billing alert webhook: channel disabled, tenantId={}", tenantId);
            return;
        }
        String url = channel.getWebhookUrl();
        if (!PublicHttpUrls.isSafeWebhookUrl(url)) {
            log.warn("Skip billing alert webhook: unsafe url, tenantId={}", tenantId);
            return;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("event", "billing.token_quota");
            payload.put("tenantId", tenantId);
            payload.put("alertName", alert.getAlertName());
            payload.put("thresholdPercent", alert.getThresholdPercent());
            payload.put("usedPercent", usedPercent);
            payload.put("usedTokens", usedTokens);
            payload.put("quota", quota);
            payload.put("title", title);
            payload.put("content", content);
            payload.put("occurredAt", LocalDateTime.now().toString());
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
                log.warn("Billing alert webhook returned {}, tenantId={}", response.statusCode(), tenantId);
            }
        } catch (Exception e) {
            log.warn("Failed to send billing alert webhook, tenantId={}", tenantId, e);
        }
    }

    static String hmacSha256(String secret, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
