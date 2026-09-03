package ai.novaflow.server.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Open API 安全端到端集成测试（Testcontainers：MySQL + Redis）。
 */
@Tag("testcontainers")
@AutoConfigureMockMvc
class OpenApiSecurityTestcontainersIntegrationTest extends AbstractTestcontainersIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void embedTokenCanAccessWelcome() throws Exception {
        OpenApiSecurityScenarios.embedTokenCanAccessWelcome(mockMvc, restTemplate);
    }

    @Test
    void embedTokenCannotListConversations() throws Exception {
        OpenApiSecurityScenarios.embedTokenCannotListConversations(mockMvc, restTemplate);
    }

    @Test
    void embedTokenCannotListMessages() throws Exception {
        OpenApiSecurityScenarios.embedTokenCannotListMessages(mockMvc, restTemplate, jdbcTemplate);
    }

    @Test
    void apiKeyRejectsInvalidCallerIdOnConversationList() throws Exception {
        OpenApiSecurityScenarios.apiKeyRejectsInvalidCallerIdOnConversationList(mockMvc, restTemplate);
    }

    @Test
    void apiKeyRejectsChatWithoutCallerId() throws Exception {
        OpenApiSecurityScenarios.apiKeyRejectsChatWithoutCallerId(mockMvc, restTemplate);
    }

    @Test
    void apiKeyListsOnlyOwnCallerConversations() throws Exception {
        OpenApiSecurityScenarios.apiKeyListsOnlyOwnCallerConversations(mockMvc, restTemplate, jdbcTemplate);
    }

    @Test
    void apiKeyCannotReadOtherCallerMessages() throws Exception {
        OpenApiSecurityScenarios.apiKeyCannotReadOtherCallerMessages(mockMvc, restTemplate, jdbcTemplate);
    }
}
