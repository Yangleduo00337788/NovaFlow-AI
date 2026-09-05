package ai.novaflow.server.integration;

import ai.novaflow.agent.domain.AgentStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

/**
 * AG-10：凭证有效但 Agent 已下线时，Open API 应返回 40302。
 */
@Tag("local")
@Execution(ExecutionMode.SAME_THREAD)
class UnpublishedAgentOpenApiLocalIntegrationTest extends AbstractLocalIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void offlineAgentRejectsOpenApiWithValidKey() {
        OpenApiIntegrationFixtures.PublishedAgent agent =
                OpenApiIntegrationFixtures.createAndPublishChatAgent(restTemplate);

        jdbcTemplate.update("UPDATE agent SET status = ? WHERE id = ?", AgentStatus.OFFLINE, agent.agentId());

        ResponseEntity<Map> welcome = restTemplate.exchange(
                "/api/v1/open/agents/" + agent.agentId() + "/welcome",
                HttpMethod.GET,
                new HttpEntity<>(null, OpenApiIntegrationFixtures.apiKeyHeaders(agent.apiKey())),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiCode(welcome, 40302);
    }
}
