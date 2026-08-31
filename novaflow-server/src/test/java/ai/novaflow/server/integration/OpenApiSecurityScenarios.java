package ai.novaflow.server.integration;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Open API 安全集成测试场景（Testcontainers / 本机环境共用）。
 */
public final class OpenApiSecurityScenarios {

    private static final String CALLER_A = "caller-user-a1";
    private static final String CALLER_B = "caller-user-b1";
    private static final String CONV_KEY_A = "e2e-conv-caller-a";
    private static final String CONV_KEY_B = "e2e-conv-caller-b";

    private OpenApiSecurityScenarios() {
    }

    public static void embedTokenCanAccessWelcome(TestRestTemplate restTemplate) {
        OpenApiIntegrationFixtures.PublishedAgent agent =
                OpenApiIntegrationFixtures.createAndPublishChatAgent(restTemplate);
        String path = "/api/v1/open/agents/" + agent.agentId() + "/welcome";
        ResponseEntity<Map> response = OpenApiIntegrationFixtures.getOpenApi(
                restTemplate,
                path,
                OpenApiIntegrationFixtures.embedTokenHeaders(agent.embedToken())
        );
        OpenApiIntegrationFixtures.assertApiSuccess(response);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertTrue(String.valueOf(data.get("reply")).contains("E2E welcome"));
    }

    public static void embedTokenCannotListConversations(TestRestTemplate restTemplate) {
        OpenApiIntegrationFixtures.PublishedAgent agent =
                OpenApiIntegrationFixtures.createAndPublishChatAgent(restTemplate);
        String path = "/api/v1/open/agents/" + agent.agentId()
                + "/conversations?callerId=" + CALLER_A + "&page=1&pageSize=20";
        ResponseEntity<Map> response = OpenApiIntegrationFixtures.getOpenApi(
                restTemplate,
                path,
                OpenApiIntegrationFixtures.embedTokenHeaders(agent.embedToken())
        );
        OpenApiIntegrationFixtures.assertApiCode(response, 40303);
    }

    public static void embedTokenCannotListMessages(
            TestRestTemplate restTemplate,
            JdbcTemplate jdbcTemplate) {
        OpenApiIntegrationFixtures.PublishedAgent agent =
                OpenApiIntegrationFixtures.createAndPublishChatAgent(restTemplate);
        OpenApiConversationSeeder.seedConversation(
                jdbcTemplate, agent.tenantId(), agent.agentId(), CALLER_A, CONV_KEY_A, "hello A", "reply A");
        String path = "/api/v1/open/agents/" + agent.agentId()
                + "/conversations/messages?conversationKey=" + CONV_KEY_A
                + "&callerId=" + CALLER_A;
        ResponseEntity<Map> response = OpenApiIntegrationFixtures.getOpenApi(
                restTemplate,
                path,
                OpenApiIntegrationFixtures.embedTokenHeaders(agent.embedToken())
        );
        OpenApiIntegrationFixtures.assertApiCode(response, 40303);
    }

    public static void apiKeyRejectsInvalidCallerIdOnConversationList(TestRestTemplate restTemplate) {
        OpenApiIntegrationFixtures.PublishedAgent agent =
                OpenApiIntegrationFixtures.createAndPublishChatAgent(restTemplate);
        String path = "/api/v1/open/agents/" + agent.agentId()
                + "/conversations?callerId=short&page=1&pageSize=20";
        ResponseEntity<Map> response = OpenApiIntegrationFixtures.getOpenApi(
                restTemplate,
                path,
                OpenApiIntegrationFixtures.apiKeyHeaders(agent.apiKey())
        );
        OpenApiIntegrationFixtures.assertApiCode(response, 40001);
    }

    public static void apiKeyRejectsChatWithoutCallerId(TestRestTemplate restTemplate) {
        OpenApiIntegrationFixtures.PublishedAgent agent =
                OpenApiIntegrationFixtures.createAndPublishChatAgent(restTemplate);
        Map<String, Object> body = new HashMap<>();
        body.put("message", "hello");
        body.put("conversationKey", "e2e-no-caller");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/open/agents/" + agent.agentId() + "/chat",
                HttpMethod.POST,
                new HttpEntity<>(body, OpenApiIntegrationFixtures.embedTokenHeaders(agent.embedToken())),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiCode(response, 40001);
    }

    public static void apiKeyListsOnlyOwnCallerConversations(
            TestRestTemplate restTemplate,
            JdbcTemplate jdbcTemplate) {
        OpenApiIntegrationFixtures.PublishedAgent agent =
                OpenApiIntegrationFixtures.createAndPublishChatAgent(restTemplate);
        OpenApiConversationSeeder.seedConversation(
                jdbcTemplate, agent.tenantId(), agent.agentId(), CALLER_A, CONV_KEY_A, "hello from A", "reply to A");
        OpenApiConversationSeeder.seedConversation(
                jdbcTemplate, agent.tenantId(), agent.agentId(), CALLER_B, CONV_KEY_B, "hello from B", "reply to B");

        String pathA = "/api/v1/open/agents/" + agent.agentId()
                + "/conversations?callerId=" + CALLER_A + "&page=1&pageSize=20";
        List<Map<String, Object>> listA = OpenApiIntegrationFixtures.extractConversationList(
                OpenApiIntegrationFixtures.getOpenApi(
                        restTemplate,
                        pathA,
                        OpenApiIntegrationFixtures.apiKeyHeaders(agent.apiKey())
                )
        );
        assertEquals(1, listA.size());
        assertEquals(CONV_KEY_A, listA.get(0).get("conversationKey"));

        String pathB = "/api/v1/open/agents/" + agent.agentId()
                + "/conversations?callerId=" + CALLER_B + "&page=1&pageSize=20";
        List<Map<String, Object>> listB = OpenApiIntegrationFixtures.extractConversationList(
                OpenApiIntegrationFixtures.getOpenApi(
                        restTemplate,
                        pathB,
                        OpenApiIntegrationFixtures.apiKeyHeaders(agent.apiKey())
                )
        );
        assertEquals(1, listB.size());
        assertEquals(CONV_KEY_B, listB.get(0).get("conversationKey"));
    }

    public static void apiKeyCannotReadOtherCallerMessages(
            TestRestTemplate restTemplate,
            JdbcTemplate jdbcTemplate) {
        OpenApiIntegrationFixtures.PublishedAgent agent =
                OpenApiIntegrationFixtures.createAndPublishChatAgent(restTemplate);
        OpenApiConversationSeeder.seedConversation(
                jdbcTemplate, agent.tenantId(), agent.agentId(), CALLER_A, CONV_KEY_A, "secret from A", "reply A");

        String path = "/api/v1/open/agents/" + agent.agentId()
                + "/conversations/messages?conversationKey=" + CONV_KEY_A
                + "&callerId=" + CALLER_B;
        ResponseEntity<Map> response = OpenApiIntegrationFixtures.getOpenApi(
                restTemplate,
                path,
                OpenApiIntegrationFixtures.apiKeyHeaders(agent.apiKey())
        );
        assertTrue(response.getBody() != null);
        int code = ((Number) response.getBody().get("code")).intValue();
        assertTrue(code != 0, "caller B should not read caller A conversation");
    }
}
