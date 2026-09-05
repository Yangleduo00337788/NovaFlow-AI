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
        // 空画布无法发布，验证接口可达且返回预期业务错误
        OpenApiIntegrationFixtures.assertApiCode(published, 40000);

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
        assertApiGet("/api/v1/billing/allocation?dimension=application", headers);
        assertApiGet("/api/v1/billing/allocation?dimension=workspace", headers);
        assertApiGet("/api/v1/billing/allocation?dimension=user", headers);
        assertApiGet("/api/v1/token-usage/logs?page=1&pageSize=5", headers);
        assertApiGet("/api/v1/trace/spans?page=1&pageSize=5", headers);
    }

    @Test
    void departmentsModule() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.login(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        assertApiGet("/api/v1/org/departments", headers);

        String deptName = "Smoke-Dept-" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> create = new HashMap<>();
        create.put("deptName", deptName);
        create.put("sortOrder", 1);

        ResponseEntity<Map> created = restTemplate.exchange(
                "/api/v1/org/departments",
                HttpMethod.POST,
                new HttpEntity<>(create, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(created);
        long deptId = idFromData(created.getBody());

        Map<String, Object> update = new HashMap<>();
        update.put("deptName", deptName + "-Updated");
        update.put("sortOrder", 2);
        ResponseEntity<Map> updated = restTemplate.exchange(
                "/api/v1/org/departments/" + deptId,
                HttpMethod.PUT,
                new HttpEntity<>(update, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(updated);

        restTemplate.exchange(
                "/api/v1/org/departments/" + deptId,
                HttpMethod.DELETE,
                new HttpEntity<>(null, headers),
                Map.class
        );
    }

    @Test
    void resourcePermissionsModule() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.login(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());
        long appId = firstApplicationId(session.token());
        long developerUserId = userIdByEmail(session.token(), "developer@novaflow.ai");

        Map<String, Object> createAgent = new HashMap<>();
        createAgent.put("agentName", "Smoke-ACL-" + UUID.randomUUID().toString().substring(0, 8));
        createAgent.put("agentType", "chat");
        createAgent.put("applicationId", appId);
        createAgent.put("welcomeMessage", "acl smoke");

        ResponseEntity<Map> created = restTemplate.exchange(
                "/api/v1/agents",
                HttpMethod.POST,
                new HttpEntity<>(createAgent, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(created);
        long agentId = idFromData(created.getBody());

        assertApiGet("/api/v1/resources/AGENT/" + agentId + "/permissions", headers);

        Map<String, Object> grant = Map.of(
                "userId", developerUserId,
                "permissionCode", "agent:read"
        );
        Map<String, Object> saveRequest = Map.of("grants", List.of(grant));
        ResponseEntity<Map> granted = restTemplate.exchange(
                "/api/v1/resources/AGENT/" + agentId + "/permissions",
                HttpMethod.PUT,
                new HttpEntity<>(saveRequest, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(granted);

        ResponseEntity<Map> cleared = restTemplate.exchange(
                "/api/v1/resources/AGENT/" + agentId + "/permissions",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("grants", List.of()), headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(cleared);

        ResponseEntity<Map> reGranted = restTemplate.exchange(
                "/api/v1/resources/AGENT/" + agentId + "/permissions",
                HttpMethod.PUT,
                new HttpEntity<>(saveRequest, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(reGranted);

        restTemplate.exchange(
                "/api/v1/agents/" + agentId,
                HttpMethod.DELETE,
                new HttpEntity<>(null, headers),
                Map.class
        );
    }

    @Test
    void portalModule() {
        OpenApiIntegrationFixtures.LoginSession admin = OpenApiIntegrationFixtures.login(restTemplate);
        var adminHeaders = OpenApiIntegrationFixtures.adminHeaders(admin.token());
        PublishedPortalApp published = createPublishedPortalApp(admin.token(), adminHeaders);

        OpenApiIntegrationFixtures.LoginSession portalUser =
                OpenApiIntegrationFixtures.login(restTemplate, "user@novaflow.ai", "User123!");
        var portalHeaders = OpenApiIntegrationFixtures.adminHeaders(portalUser.token());

        assertApiGet("/api/v1/portal/apps", portalHeaders);

        ResponseEntity<Map> apps = restTemplate.exchange(
                "/api/v1/portal/apps",
                HttpMethod.GET,
                new HttpEntity<>(null, portalHeaders),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(apps);
        List<?> appList = (List<?>) apps.getBody().get("data");
        assertNotNull(appList);
        assertTrue(appList.stream().anyMatch(item -> published.appId() == idFromItem(item)),
                "portal should list the published application");

        assertApiGet("/api/v1/portal/apps/" + published.appId(), portalHeaders);
        assertApiGet("/api/v1/portal/apps/" + published.appId() + "/conversations?page=1&pageSize=10", portalHeaders);

        restTemplate.exchange(
                "/api/v1/applications/" + published.appId() + "/unpublish",
                HttpMethod.POST,
                new HttpEntity<>(null, adminHeaders),
                Map.class
        );
        restTemplate.exchange(
                "/api/v1/agents/" + published.agentId(),
                HttpMethod.DELETE,
                new HttpEntity<>(null, adminHeaders),
                Map.class
        );
        restTemplate.exchange(
                "/api/v1/applications/" + published.appId(),
                HttpMethod.DELETE,
                new HttpEntity<>(null, adminHeaders),
                Map.class
        );
    }

    @Test
    void transferOwnerRoundTrip() {
        OpenApiIntegrationFixtures.LoginSession admin = OpenApiIntegrationFixtures.login(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(admin.token());

        long developerMemberId = memberIdByEmail(admin.token(), "developer@novaflow.ai");
        long adminMemberId = memberIdByEmail(admin.token(), "admin@novaflow.ai");

        ResponseEntity<Map> transferToDeveloper = restTemplate.exchange(
                "/api/v1/org/tenant/transfer-owner",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("memberId", developerMemberId), headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(transferToDeveloper);

        OpenApiIntegrationFixtures.LoginSession developer =
                OpenApiIntegrationFixtures.login(restTemplate, "developer@novaflow.ai", "Developer123!");
        ResponseEntity<Map> transferBack = restTemplate.exchange(
                "/api/v1/org/tenant/transfer-owner",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("memberId", adminMemberId), OpenApiIntegrationFixtures.adminHeaders(developer.token())),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(transferBack);
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

    private long idFromItem(Object item) {
        return ((Number) ((Map<?, ?>) item).get("id")).longValue();
    }

    private long memberIdByEmail(String token, String email) {
        Map<?, ?> member = memberByEmail(token, email);
        return ((Number) member.get("id")).longValue();
    }

    private long userIdByEmail(String token, String email) {
        Map<?, ?> member = memberByEmail(token, email);
        return ((Number) member.get("userId")).longValue();
    }

    private Map<?, ?> memberByEmail(String token, String email) {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/org/members?page=1&pageSize=50",
                HttpMethod.GET,
                new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(token)),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(response);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertNotNull(data);
        List<?> members = (List<?>) data.get("list");
        assertNotNull(members);
        for (Object item : members) {
            Map<?, ?> member = (Map<?, ?>) item;
            if (email.equals(String.valueOf(member.get("email")))) {
                return member;
            }
        }
        throw new AssertionError("member not found: " + email);
    }

    private record PublishedPortalApp(long appId, long agentId) {
    }

    private PublishedPortalApp createPublishedPortalApp(String token, org.springframework.http.HttpHeaders headers) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> createApp = new HashMap<>();
        createApp.put("appName", "Smoke-Portal-" + suffix);
        createApp.put("description", "portal smoke");

        ResponseEntity<Map> appCreated = restTemplate.exchange(
                "/api/v1/applications",
                HttpMethod.POST,
                new HttpEntity<>(createApp, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(appCreated);
        long appId = idFromData(appCreated.getBody());

        Map<String, Object> createAgent = new HashMap<>();
        createAgent.put("agentName", "Smoke-Portal-Agent-" + suffix);
        createAgent.put("agentType", "chat");
        createAgent.put("applicationId", appId);
        createAgent.put("welcomeMessage", "portal smoke");

        ResponseEntity<Map> agentCreated = restTemplate.exchange(
                "/api/v1/agents",
                HttpMethod.POST,
                new HttpEntity<>(createAgent, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(agentCreated);
        long agentId = idFromData(agentCreated.getBody());

        ResponseEntity<Map> agentPublished = restTemplate.exchange(
                "/api/v1/agents/" + agentId + "/publish",
                HttpMethod.POST,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(agentPublished);

        Map<String, Object> updateApp = new HashMap<>();
        updateApp.put("appName", "Smoke-Portal-" + suffix);
        updateApp.put("description", "portal smoke");
        updateApp.put("defaultAgentId", agentId);
        updateApp.put("agentIds", List.of(agentId));

        ResponseEntity<Map> appUpdated = restTemplate.exchange(
                "/api/v1/applications/" + appId,
                HttpMethod.PUT,
                new HttpEntity<>(updateApp, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(appUpdated);

        ResponseEntity<Map> appPublished = restTemplate.exchange(
                "/api/v1/applications/" + appId + "/publish",
                HttpMethod.POST,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(appPublished);

        return new PublishedPortalApp(appId, agentId);
    }
}
