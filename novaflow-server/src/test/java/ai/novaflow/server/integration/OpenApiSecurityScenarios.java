package ai.novaflow.server.integration;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    public static void embedTokenCanAccessWelcome(MockMvc mockMvc, TestRestTemplate restTemplate) throws Exception {
        OpenApiIntegrationFixtures.PublishedAgent agent =
                OpenApiIntegrationFixtures.createAndPublishChatAgent(restTemplate);
        OpenApiIntegrationFixtures.assertApiSuccess(
                OpenApiIntegrationFixtures.getOpenApi(
                        restTemplate,
                        "/api/v1/open/agents/" + agent.agentId() + "/welcome",
                        OpenApiIntegrationFixtures.embedTokenHeaders(agent.embedToken())
                )
        );
    }

    public static void embedTokenCannotListConversations(MockMvc mockMvc, TestRestTemplate restTemplate) throws Exception {
        OpenApiIntegrationFixtures.PublishedAgent agent =
                OpenApiIntegrationFixtures.createAndPublishChatAgent(restTemplate);
        assertErrorCode(mockMvc.perform(get("/api/v1/open/agents/{id}/conversations", agent.agentId())
                .param("callerId", CALLER_A)
                .param("page", "1")
                .param("pageSize", "20")
                .header("X-Embed-Token", agent.embedToken())), 40303);
    }

    public static void embedTokenCannotListMessages(
            MockMvc mockMvc,
            TestRestTemplate restTemplate,
            JdbcTemplate jdbcTemplate) throws Exception {
        OpenApiIntegrationFixtures.PublishedAgent agent =
                OpenApiIntegrationFixtures.createAndPublishChatAgent(restTemplate);
        OpenApiConversationSeeder.seedConversation(
                jdbcTemplate, agent.tenantId(), agent.agentId(), CALLER_A, CONV_KEY_A, "hello A", "reply A");
        assertErrorCode(mockMvc.perform(get("/api/v1/open/agents/{id}/conversations/messages", agent.agentId())
                .param("conversationKey", CONV_KEY_A)
                .param("callerId", CALLER_A)
                .header("X-Embed-Token", agent.embedToken())), 40303);
    }

    public static void apiKeyRejectsInvalidCallerIdOnConversationList(
            MockMvc mockMvc,
            TestRestTemplate restTemplate) throws Exception {
        OpenApiIntegrationFixtures.PublishedAgent agent =
                OpenApiIntegrationFixtures.createAndPublishChatAgent(restTemplate);
        assertErrorCode(mockMvc.perform(get("/api/v1/open/agents/{id}/conversations", agent.agentId())
                .param("callerId", "short")
                .param("page", "1")
                .param("pageSize", "20")
                .header("Authorization", "Bearer " + agent.apiKey())), 40001);
    }

    public static void apiKeyRejectsChatWithoutCallerId(MockMvc mockMvc, TestRestTemplate restTemplate) throws Exception {
        OpenApiIntegrationFixtures.PublishedAgent agent =
                OpenApiIntegrationFixtures.createAndPublishChatAgent(restTemplate);
        assertErrorCode(mockMvc.perform(post("/api/v1/open/agents/{id}/chat", agent.agentId())
                .header("X-Embed-Token", agent.embedToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"hello\",\"conversationKey\":\"e2e-no-caller\"}")), 40001);
    }

    public static void apiKeyListsOnlyOwnCallerConversations(
            MockMvc mockMvc,
            TestRestTemplate restTemplate,
            JdbcTemplate jdbcTemplate) throws Exception {
        OpenApiIntegrationFixtures.PublishedAgent agent =
                OpenApiIntegrationFixtures.createAndPublishChatAgent(restTemplate);
        OpenApiConversationSeeder.seedConversation(
                jdbcTemplate, agent.tenantId(), agent.agentId(), CALLER_A, CONV_KEY_A, "hello from A", "reply to A");
        OpenApiConversationSeeder.seedConversation(
                jdbcTemplate, agent.tenantId(), agent.agentId(), CALLER_B, CONV_KEY_B, "hello from B", "reply to B");

        List<Map<String, Object>> listA = extractConversationList(
                mockMvc.perform(get("/api/v1/open/agents/{id}/conversations", agent.agentId())
                        .param("callerId", CALLER_A)
                        .param("page", "1")
                        .param("pageSize", "20")
                        .header("Authorization", "Bearer " + agent.apiKey())));
        assertEquals(1, listA.size());
        assertEquals(CONV_KEY_A, listA.get(0).get("conversationKey"));

        List<Map<String, Object>> listB = extractConversationList(
                mockMvc.perform(get("/api/v1/open/agents/{id}/conversations", agent.agentId())
                        .param("callerId", CALLER_B)
                        .param("page", "1")
                        .param("pageSize", "20")
                        .header("Authorization", "Bearer " + agent.apiKey())));
        assertEquals(1, listB.size());
        assertEquals(CONV_KEY_B, listB.get(0).get("conversationKey"));
    }

    public static void apiKeyCannotReadOtherCallerMessages(
            MockMvc mockMvc,
            TestRestTemplate restTemplate,
            JdbcTemplate jdbcTemplate) throws Exception {
        OpenApiIntegrationFixtures.PublishedAgent agent =
                OpenApiIntegrationFixtures.createAndPublishChatAgent(restTemplate);
        OpenApiConversationSeeder.seedConversation(
                jdbcTemplate, agent.tenantId(), agent.agentId(), CALLER_A, CONV_KEY_A, "secret from A", "reply A");

        String body = mockMvc.perform(get("/api/v1/open/agents/{id}/conversations/messages", agent.agentId())
                        .param("conversationKey", CONV_KEY_A)
                        .param("callerId", CALLER_B)
                        .header("Authorization", "Bearer " + agent.apiKey()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertTrue(body.contains("\"code\":") && !body.contains("\"code\":0"),
                "caller B should not read caller A conversation");
    }

    private static void assertErrorCode(ResultActions actions, int expectedCode) throws Exception {
        var result = actions.andExpect(jsonPath("$.code").value(expectedCode)).andReturn();
        int actualStatus = result.getResponse().getStatus();
        if (actualStatus != 200) {
            assertEquals(
                    OpenApiIntegrationFixtures.httpStatusOf(expectedCode).value(),
                    actualStatus,
                    () -> "body.code=" + expectedCode + " http=" + actualStatus);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> extractConversationList(ResultActions actions) throws Exception {
        String json = actions.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertTrue(json.contains("\"conversationKey\""));
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var root = mapper.readValue(json, Map.class);
        Map<?, ?> data = (Map<?, ?>) root.get("data");
        return (List<Map<String, Object>>) data.get("list");
    }
}
