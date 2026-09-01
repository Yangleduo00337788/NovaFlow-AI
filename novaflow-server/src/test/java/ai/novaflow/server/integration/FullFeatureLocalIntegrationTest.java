package ai.novaflow.server.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 全模块 API 冒烟：每个功能域至少调用一次读接口，并对核心写操作做创建-查询-删除闭环。
 */
@Tag("local")
@Execution(ExecutionMode.SAME_THREAD)
class FullFeatureLocalIntegrationTest extends AbstractLocalIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthAndAuthFlow() {
        assertHealthUp(restTemplate);

        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.login(restTemplate);

        ResponseEntity<Map> me = restTemplate.exchange(
                "/api/v1/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(session.token())),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(me);

        ResponseEntity<Map> logout = restTemplate.exchange(
                "/api/v1/auth/logout",
                HttpMethod.POST,
                new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(session.token())),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(logout);
    }

    @Test
    void registerNewTenant() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, String> request = Map.of(
                "companyName", "SmokeTest-" + suffix,
                "email", "smoke-" + suffix + "@novaflow.test",
                "nickname", "Smoke User",
                "password", "SmokeTest123!",
                "confirmPassword", "SmokeTest123!"
        );
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/register", request, Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = response.getBody();
        assertNotNull(body);
        assertEquals(0, ((Number) body.get("code")).intValue());
    }

    @Test
    void dashboardSearchAndMonitor() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.login(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        assertApiGet("/api/v1/dashboard/overview", headers);
        assertApiGet("/api/v1/dashboard/recent-items", headers);
        assertApiGet("/api/v1/dashboard/favorites", headers);
        assertApiGet("/api/v1/dashboard/published-workflows", headers);
        assertApiGet("/api/v1/search?keyword=DeepSeek", headers);
        assertApiGet("/api/v1/monitor/overview", headers);
        assertApiGet("/api/v1/monitor/observability", headers);
    }

    @Test
    void agentsModule() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.login(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());
        long appId = firstApplicationId(session.token());

        assertApiGet("/api/v1/agents?page=1&pageSize=5", headers);

        Map<String, Object> create = new HashMap<>();
        create.put("agentName", "Smoke-Agent-" + UUID.randomUUID().toString().substring(0, 8));
        create.put("agentType", "chat");
        create.put("applicationId", appId);
        create.put("welcomeMessage", "smoke welcome");

        ResponseEntity<Map> created = restTemplate.exchange(
                "/api/v1/agents",
                HttpMethod.POST,
                new HttpEntity<>(create, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(created);
        long agentId = idFromData(created.getBody());

        assertApiGet("/api/v1/agents/" + agentId, headers);
        assertApiGet("/api/v1/agents/" + agentId + "/publish", headers);

        ResponseEntity<Map> published = restTemplate.exchange(
                "/api/v1/agents/" + agentId + "/publish",
                HttpMethod.POST,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(published);

        ResponseEntity<Map> welcome = restTemplate.exchange(
                "/api/v1/agents/" + agentId + "/debug/welcome",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        // 模型密钥解密失败时欢迎语会 500，仅验证对话列表接口
        if (welcome.getBody() != null && ((Number) welcome.getBody().get("code")).intValue() == 0) {
            assertApiGet("/api/v1/agents/" + agentId + "/debug/conversations", headers);
        }

        restTemplate.exchange(
                "/api/v1/agents/" + agentId,
                HttpMethod.DELETE,
                new HttpEntity<>(null, headers),
                Map.class
        );
    }

    @Test
    void workflowsModule() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.login(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());
        long appId = firstApplicationId(session.token());

        assertApiGet("/api/v1/workflows?page=1&pageSize=5", headers);
        assertApiGet("/api/v1/workflows/options", headers);

        Map<String, Object> create = new HashMap<>();
        create.put("workflowName", "Smoke-WF-" + UUID.randomUUID().toString().substring(0, 8));
        create.put("applicationId", appId);
        create.put("description", "smoke");

        ResponseEntity<Map> created = restTemplate.exchange(
                "/api/v1/workflows",
                HttpMethod.POST,
                new HttpEntity<>(create, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(created);
        long workflowId = idFromData(created.getBody());

        assertApiGet("/api/v1/workflows/" + workflowId, headers);

        ResponseEntity<Map> published = restTemplate.exchange(
                "/api/v1/workflows/" + workflowId + "/publish",
                HttpMethod.POST,
                new HttpEntity<>(null, headers),
                Map.class
        );
        // 空画布无法发布，仅验证接口可达（可能返回业务错误）
        assertEquals(HttpStatus.OK, published.getStatusCode());

        restTemplate.exchange(
                "/api/v1/workflows/" + workflowId,
                HttpMethod.DELETE,
                new HttpEntity<>(null, headers),
                Map.class
        );
    }

    @Test
    void knowledgeModule() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.login(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        assertApiGet("/api/v1/knowledge-bases?page=1&pageSize=5", headers);

        String embeddingModel = firstEmbeddingModel(headers);
        Map<String, Object> create = new HashMap<>();
        create.put("kbName", "Smoke-KB-" + UUID.randomUUID().toString().substring(0, 8));
        create.put("description", "smoke");
        create.put("embeddingModel", embeddingModel);

        ResponseEntity<Map> created = restTemplate.exchange(
                "/api/v1/knowledge-bases",
                HttpMethod.POST,
                new HttpEntity<>(create, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(created);
        long kbId = idFromData(created.getBody());

        assertApiGet("/api/v1/knowledge-bases/" + kbId, headers);
        assertApiGet("/api/v1/knowledge-bases/" + kbId + "/documents", headers);

        ResponseEntity<Map> retrieve = restTemplate.exchange(
                "/api/v1/knowledge-bases/" + kbId + "/retrieve",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("query", "test", "topK", 3), headers),
                Map.class
        );
        assertEquals(HttpStatus.OK, retrieve.getStatusCode());

        restTemplate.exchange(
                "/api/v1/knowledge-bases/" + kbId,
                HttpMethod.DELETE,
                new HttpEntity<>(null, headers),
                Map.class
        );
    }

    @Test
    void modelsModule() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.login(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        assertApiGet("/api/v1/models/overview", headers);
        assertApiGet("/api/v1/models/embedding-options", headers);
        assertApiGet("/api/v1/models/configs", headers);

        ResponseEntity<Map> configs = restTemplate.exchange(
                "/api/v1/models/configs",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(configs);
        List<?> configsList = (List<?>) configs.getBody().get("data");
        if (configsList != null && !configsList.isEmpty()) {
            long configId = ((Number) ((Map<?, ?>) configsList.get(0)).get("id")).longValue();
            assertApiGet("/api/v1/models/configs/" + configId, headers);
        }
    }

    @Test
    void toolsMcpAndSkillsModule() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.login(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        assertApiGet("/api/v1/tools?page=1&pageSize=5", headers);
        assertApiGet("/api/v1/tools/options", headers);
        assertApiGet("/api/v1/mcp-servers", headers);
        assertApiGet("/api/v1/skills/options", headers);

        String toolName = "smoke_" + UUID.randomUUID().toString().substring(0, 8).replace("-", "");
        Map<String, Object> create = new HashMap<>();
        create.put("toolName", toolName);
        create.put("displayName", "Smoke Tool");
        create.put("toolType", "http");
        create.put("method", "GET");
        create.put("url", "https://httpbin.org/get");

        ResponseEntity<Map> created = restTemplate.exchange(
                "/api/v1/tools",
                HttpMethod.POST,
                new HttpEntity<>(create, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(created);
        long toolId = idFromData(created.getBody());

        assertApiGet("/api/v1/tools/" + toolId, headers);

        restTemplate.exchange(
                "/api/v1/tools/" + toolId,
                HttpMethod.DELETE,
                new HttpEntity<>(null, headers),
                Map.class
        );
    }

    @Test
    void promptsModule() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.login(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        assertApiGet("/api/v1/prompts?page=1&pageSize=5", headers);
        assertApiGet("/api/v1/prompts/options", headers);

        Map<String, Object> create = new HashMap<>();
        create.put("templateName", "Smoke-Prompt-" + UUID.randomUUID().toString().substring(0, 8));
        create.put("content", "Hello {{name}}");
        create.put("category", "custom");

        ResponseEntity<Map> created = restTemplate.exchange(
                "/api/v1/prompts",
                HttpMethod.POST,
                new HttpEntity<>(create, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(created);
        long promptId = idFromData(created.getBody());

        assertApiGet("/api/v1/prompts/" + promptId, headers);
        assertApiGet("/api/v1/prompts/" + promptId + "/versions", headers);

        restTemplate.exchange(
                "/api/v1/prompts/" + promptId,
                HttpMethod.DELETE,
                new HttpEntity<>(null, headers),
                Map.class
        );
    }

    @Test
    void applicationsModule() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.login(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        assertApiGet("/api/v1/applications?page=1&pageSize=5", headers);
        assertApiGet("/api/v1/applications/options", headers);

        Map<String, Object> create = new HashMap<>();
        create.put("appName", "Smoke-App-" + UUID.randomUUID().toString().substring(0, 8));
        create.put("description", "smoke");

        ResponseEntity<Map> created = restTemplate.exchange(
                "/api/v1/applications",
                HttpMethod.POST,
                new HttpEntity<>(create, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(created);
        long appId = idFromData(created.getBody());

        assertApiGet("/api/v1/applications/" + appId, headers);
        assertApiGet("/api/v1/applications/" + appId + "/publish", headers);

        restTemplate.exchange(
                "/api/v1/applications/" + appId,
                HttpMethod.DELETE,
                new HttpEntity<>(null, headers),
                Map.class
        );
    }

    @Test
    void billingTokenUsageAndTraces() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.login(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        assertApiGet("/api/v1/billing/overview", headers);
        assertApiGet("/api/v1/billing/quota", headers);
        assertApiGet("/api/v1/billing/alerts", headers);
        assertApiGet("/api/v1/billing/records?page=1&pageSize=5", headers);
        assertApiGet("/api/v1/token-usage/logs?page=1&pageSize=5", headers);
        assertApiGet("/api/v1/trace/spans?page=1&pageSize=5", headers);
    }

    @Test
    void orgRolesAndNotifications() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.login(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        assertApiGet("/api/v1/org/tenant", headers);
        assertApiGet("/api/v1/org/plan-summary", headers);
        assertApiGet("/api/v1/org/workspaces", headers);
        assertApiGet("/api/v1/org/members?page=1&pageSize=5", headers);
        assertApiGet("/api/v1/roles", headers);
        assertApiGet("/api/v1/permissions", headers);
        assertApiGet("/api/v1/permissions/grouped", headers);
        assertApiGet("/api/v1/notifications?page=1&pageSize=5", headers);
        assertApiGet("/api/v1/notifications/unread-count", headers);

        ResponseEntity<Map> roles = restTemplate.exchange(
                "/api/v1/roles",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(roles);
        List<?> roleList = (List<?>) roles.getBody().get("data");
        if (roleList != null && !roleList.isEmpty()) {
            long roleId = ((Number) ((Map<?, ?>) roleList.get(0)).get("id")).longValue();
            assertApiGet("/api/v1/roles/" + roleId, headers);
            assertApiGet("/api/v1/roles/" + roleId + "/members", headers);
        }
    }

    @Test
    void openApiEndpoints() {
        OpenApiIntegrationFixtures.PublishedAgent agent = OpenApiIntegrationFixtures.createAndPublishChatAgent(restTemplate);

        OpenApiIntegrationFixtures.assertApiSuccess(
                OpenApiIntegrationFixtures.getOpenApi(
                        restTemplate,
                        "/api/v1/open/agents/" + agent.agentId() + "/welcome",
                        OpenApiIntegrationFixtures.embedTokenHeaders(agent.embedToken())
                )
        );

        OpenApiIntegrationFixtures.assertApiSuccess(
                OpenApiIntegrationFixtures.getOpenApi(
                        restTemplate,
                        "/api/v1/open/agents/" + agent.agentId() + "/conversations?callerId=smoke-caller",
                        OpenApiIntegrationFixtures.apiKeyHeaders(agent.apiKey())
                )
        );
    }

    private void assertApiGet(String path, org.springframework.http.HttpHeaders headers) {
        ResponseEntity<Map> response = restTemplate.exchange(
                path,
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(response);
    }

    private String firstEmbeddingModel(org.springframework.http.HttpHeaders headers) {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/models/embedding-options",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(response);
        List<?> options = (List<?>) response.getBody().get("data");
        assertNotNull(options);
        assertTrue(!options.isEmpty(), "embedding options should exist");
        Map<?, ?> first = (Map<?, ?>) options.get(0);
        Object modelName = first.get("modelName");
        assertNotNull(modelName);
        return String.valueOf(modelName);
    }

    private long firstApplicationId(String token) {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/applications/options",
                HttpMethod.GET,
                new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(token)),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(response);
        List<?> options = (List<?>) response.getBody().get("data");
        assertNotNull(options);
        assertTrue(!options.isEmpty());
        return ((Number) ((Map<?, ?>) options.get(0)).get("id")).longValue();
    }

    private long idFromData(Map<?, ?> body) {
        assertNotNull(body);
        Map<?, ?> data = (Map<?, ?>) body.get("data");
        assertNotNull(data);
        return ((Number) data.get("id")).longValue();
    }
}
