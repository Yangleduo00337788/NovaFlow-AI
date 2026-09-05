package ai.novaflow.server.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Token 月度配额：API 级验证预占失败时 Agent 调试对话被拒绝。
 */
@Tag("local")
@Execution(ExecutionMode.SAME_THREAD)
class TokenQuotaLocalIntegrationTest extends AbstractLocalIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void debugChatRejectedWhenMonthlyTokenQuotaExceeded() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        OpenApiIntegrationFixtures.LoginSession session =
                OpenApiIntegrationFixtures.registerNewTenant(restTemplate, "quota-" + suffix);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        long appId = OpenApiIntegrationFixtures.createApplication(
                restTemplate, session.token(), "Quota-App-" + suffix);
        long agentId = OpenApiIntegrationFixtures.createChatAgent(
                restTemplate, session.token(), appId, "Quota-Agent-" + suffix);

        ResponseEntity<Map> quotaResponse = restTemplate.exchange(
                "/api/v1/billing/quota",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("monthlyTokenQuota", 1L), headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(quotaResponse);

        String quotaKey = monthlyQuotaKey(session.tenantId());
        stringRedisTemplate.opsForValue().set(quotaKey, "1");

        try {
            ResponseEntity<Map> chatResponse = restTemplate.exchange(
                    "/api/v1/agents/" + agentId + "/debug/chat",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("message", "quota integration smoke"), headers),
                    Map.class
            );
            OpenApiIntegrationFixtures.assertApiCode(chatResponse, 40000);
            Object message = chatResponse.getBody().get("message");
            assertTrue(
                    String.valueOf(message).contains("配额"),
                    () -> "expected quota message, got: " + message
            );
        } finally {
            stringRedisTemplate.delete(quotaKey);
        }
    }

    @Test
    void debugChatAllowedWhenQuotaNotExhausted() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        OpenApiIntegrationFixtures.LoginSession session =
                OpenApiIntegrationFixtures.registerNewTenant(restTemplate, "quota-ok-" + suffix);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        long appId = OpenApiIntegrationFixtures.createApplication(
                restTemplate, session.token(), "Quota-Ok-App-" + suffix);
        long agentId = OpenApiIntegrationFixtures.createChatAgent(
                restTemplate, session.token(), appId, "Quota-Ok-Agent-" + suffix);

        ResponseEntity<Map> quotaResponse = restTemplate.exchange(
                "/api/v1/billing/quota",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("monthlyTokenQuota", 1_000_000L), headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(quotaResponse);

        String quotaKey = monthlyQuotaKey(session.tenantId());
        stringRedisTemplate.delete(quotaKey);

        ResponseEntity<Map> chatResponse = restTemplate.exchange(
                "/api/v1/agents/" + agentId + "/debug/chat",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("message", "quota ok smoke"), headers),
                Map.class
        );
        Map<?, ?> body = chatResponse.getBody();
        assertTrue(body != null);
        Object message = body.get("message");
        assertTrue(
                message == null || !String.valueOf(message).contains("配额已用尽"),
                () -> "quota should not block chat, body=" + body
        );
    }

    private static String monthlyQuotaKey(long tenantId) {
        return "novaflow:quota:monthly:" + tenantId + ":" + YearMonth.now();
    }
}
