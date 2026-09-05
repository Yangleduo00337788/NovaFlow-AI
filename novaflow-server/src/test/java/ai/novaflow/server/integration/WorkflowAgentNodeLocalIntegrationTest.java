package ai.novaflow.server.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 工作流 Agent 节点画布保存与发布集成测试。
 */
@Tag("local")
class WorkflowAgentNodeLocalIntegrationTest extends AbstractLocalIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void workflowWithAgentNodeCanBePublished() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.login(restTemplate);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long appId = OpenApiIntegrationFixtures.createApplication(
                restTemplate, session.token(), "WF-Agent-IT-App-" + suffix);
        long agentId = OpenApiIntegrationFixtures.createChatAgent(
                restTemplate, session.token(), appId, "WF-Agent-IT-" + suffix);

        ResponseEntity<Map> publishAgent = restTemplate.exchange(
                "/api/v1/agents/" + agentId + "/publish",
                HttpMethod.POST,
                new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(session.token())),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(publishAgent);

        Map<String, Object> createWf = new HashMap<>();
        createWf.put("workflowName", "WF-Agent-IT-" + suffix);
        createWf.put("applicationId", appId);
        createWf.put("description", "integration test");

        ResponseEntity<Map> created = restTemplate.exchange(
                "/api/v1/workflows",
                HttpMethod.POST,
                new HttpEntity<>(createWf, OpenApiIntegrationFixtures.adminHeaders(session.token())),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(created);
        long workflowId = ((Number) ((Map<?, ?>) created.getBody().get("data")).get("id")).longValue();

        Map<String, Object> agentConfig = new HashMap<>();
        agentConfig.put("agentId", agentId);
        agentConfig.put("messageTemplate", "{{input}}");

        Map<String, Object> agentNodeData = new HashMap<>();
        agentNodeData.put("label", "Agent");
        agentNodeData.put("config", agentConfig);

        Map<String, Object> agentNode = new HashMap<>();
        agentNode.put("id", "agent-1");
        agentNode.put("type", "agent");
        agentNode.put("position", Map.of("x", 260, "y", 200));
        agentNode.put("data", agentNodeData);

        Map<String, Object> canvas = new HashMap<>();
        canvas.put("nodes", List.of(
                Map.of("id", "start-1", "type", "start", "position", Map.of("x", 80, "y", 200),
                        "data", Map.of("label", "开始")),
                agentNode,
                Map.of("id", "end-1", "type", "end", "position", Map.of("x", 440, "y", 200),
                        "data", Map.of("label", "结束"))
        ));
        canvas.put("edges", List.of(
                Map.of("id", "e1", "source", "start-1", "target", "agent-1"),
                Map.of("id", "e2", "source", "agent-1", "target", "end-1")
        ));

        Map<String, Object> updateWf = new HashMap<>();
        updateWf.put("workflowName", "WF-Agent-IT-" + suffix);
        updateWf.put("applicationId", appId);
        updateWf.put("description", "integration test");
        updateWf.put("canvasData", canvas);

        ResponseEntity<Map> updated = restTemplate.exchange(
                "/api/v1/workflows/" + workflowId,
                HttpMethod.PUT,
                new HttpEntity<>(updateWf, OpenApiIntegrationFixtures.adminHeaders(session.token())),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(updated);

        ResponseEntity<Map> published = restTemplate.exchange(
                "/api/v1/workflows/" + workflowId + "/publish",
                HttpMethod.POST,
                new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(session.token())),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(published);
        assertNotNull(published.getBody());

        restTemplate.exchange(
                "/api/v1/workflows/" + workflowId,
                HttpMethod.DELETE,
                new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(session.token())),
                Map.class
        );
        restTemplate.exchange(
                "/api/v1/agents/" + agentId,
                HttpMethod.DELETE,
                new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(session.token())),
                Map.class
        );
        restTemplate.exchange(
                "/api/v1/applications/" + appId,
                HttpMethod.DELETE,
                new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(session.token())),
                Map.class
        );
    }
}
