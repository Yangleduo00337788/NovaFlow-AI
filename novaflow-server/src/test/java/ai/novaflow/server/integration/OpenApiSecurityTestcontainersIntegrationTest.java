package ai.novaflow.server.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
/**
 * Open API 安全端到端集成测试（Testcontainers：MySQL + Redis）。
 */
@Tag("testcontainers")
class OpenApiSecurityTestcontainersIntegrationTest extends AbstractTestcontainersIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void embedTokenCanAccessWelcome() {
        OpenApiSecurityScenarios.embedTokenCanAccessWelcome(restTemplate);
    }

    @Test
    void embedTokenCannotListConversations() {
        OpenApiSecurityScenarios.embedTokenCannotListConversations(restTemplate);
    }

    @Test
    void embedTokenCannotListMessages() {
        OpenApiSecurityScenarios.embedTokenCannotListMessages(restTemplate, jdbcTemplate);
    }

    @Test
    void apiKeyRejectsInvalidCallerIdOnConversationList() {
        OpenApiSecurityScenarios.apiKeyRejectsInvalidCallerIdOnConversationList(restTemplate);
    }

    @Test
    void apiKeyRejectsChatWithoutCallerId() {
        OpenApiSecurityScenarios.apiKeyRejectsChatWithoutCallerId(restTemplate);
    }

    @Test
    void apiKeyListsOnlyOwnCallerConversations() {
        OpenApiSecurityScenarios.apiKeyListsOnlyOwnCallerConversations(restTemplate, jdbcTemplate);
    }

    @Test
    void apiKeyCannotReadOtherCallerMessages() {
        OpenApiSecurityScenarios.apiKeyCannotReadOtherCallerMessages(restTemplate, jdbcTemplate);
    }
}
