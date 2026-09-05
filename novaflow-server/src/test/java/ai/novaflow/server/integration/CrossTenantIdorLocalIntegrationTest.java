package ai.novaflow.server.integration;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 跨租户 IDOR：租户 B 不得读/改/删租户 A 的资源（Z-04 / Z-05 / Z-06）。
 */
@Tag("local")
@Execution(ExecutionMode.SAME_THREAD)
class CrossTenantIdorLocalIntegrationTest extends AbstractLocalIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void tenantBCannotAccessTenantAResources() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String emailA = "idor-a-" + suffix + "@novaflow.test";
        String emailB = "idor-b-" + suffix + "@novaflow.test";

        OpenApiIntegrationFixtures.registerNewTenant(restTemplate, "a-" + suffix);
        OpenApiIntegrationFixtures.registerNewTenant(restTemplate, "b-" + suffix);

        OpenApiIntegrationFixtures.LoginSession tenantA =
                OpenApiIntegrationFixtures.login(restTemplate, emailA, "SmokeTest123!");
        long appId = OpenApiIntegrationFixtures.createApplication(
                restTemplate, tenantA.token(), "IDOR-App-" + suffix);
        long agentId = OpenApiIntegrationFixtures.createChatAgent(
                restTemplate, tenantA.token(), appId, "IDOR-Agent-" + suffix);
        long workflowId = createWorkflow(restTemplate, tenantA.token(), appId, "IDOR-WF-" + suffix);

        // 独立客户端登录 B，避免与 A 的 Cookie 串扰
        RestTemplate attacker = isolatedClient();
        OpenApiIntegrationFixtures.LoginSession tenantB = loginOn(attacker, emailB, "SmokeTest123!");
        assertNotEquals(tenantA.tenantId(), tenantB.tenantId(), "test tenants must be isolated");

        // 确认 B 的 token 在独立客户端可用
        ResponseEntity<Map> me = attacker.exchange(
                "/api/v1/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(tenantB.token())),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(me);
        Map<?, ?> meData = (Map<?, ?>) me.getBody().get("data");
        Map<?, ?> meTenant = (Map<?, ?>) meData.get("tenant");
        assertEquals(tenantB.tenantId(), ((Number) meTenant.get("id")).longValue());

        assertCrossTenantDenied(attacker, tenantB.token(), "/api/v1/agents/" + agentId, HttpMethod.GET);
        assertCrossTenantDenied(attacker, tenantB.token(), "/api/v1/agents/" + agentId, HttpMethod.DELETE);
        assertCrossTenantDenied(attacker, tenantB.token(), "/api/v1/applications/" + appId, HttpMethod.GET);
        assertCrossTenantDenied(attacker, tenantB.token(), "/api/v1/applications/" + appId, HttpMethod.DELETE);
        assertCrossTenantDenied(attacker, tenantB.token(), "/api/v1/workflows/" + workflowId, HttpMethod.GET);
        assertCrossTenantDenied(attacker, tenantB.token(), "/api/v1/workflows/" + workflowId, HttpMethod.DELETE);

        Long kbId = tryCreateKnowledgeBase(restTemplate, tenantA.token(), "IDOR-KB-" + suffix);
        Assumptions.assumeTrue(kbId != null, "skip knowledge IDOR when embedding model is unavailable");
        assertCrossTenantDenied(attacker, tenantB.token(), "/api/v1/knowledge-bases/" + kbId, HttpMethod.GET);
        assertCrossTenantDenied(attacker, tenantB.token(), "/api/v1/knowledge-bases/" + kbId, HttpMethod.DELETE);

        ResponseEntity<Map> ownAgent = restTemplate.exchange(
                "/api/v1/agents/" + agentId,
                HttpMethod.GET,
                new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(tenantA.token())),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(ownAgent);
    }

    private Long tryCreateKnowledgeBase(TestRestTemplate restTemplate, String token, String kbName) {
        try {
            return OpenApiIntegrationFixtures.createKnowledgeBase(restTemplate, token, kbName);
        } catch (AssertionError ex) {
            return null;
        }
    }

    private long createWorkflow(
            TestRestTemplate restTemplate,
            String token,
            long applicationId,
            String workflowName
    ) {
        Map<String, Object> request = Map.of(
                "workflowName", workflowName,
                "description", "IDOR test",
                "applicationId", applicationId
        );
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/workflows",
                HttpMethod.POST,
                new HttpEntity<>(request, OpenApiIntegrationFixtures.adminHeaders(token)),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(response);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertNotNull(data);
        return ((Number) data.get("id")).longValue();
    }

    private RestTemplate isolatedClient() {
        var httpClient = HttpClients.custom().disableRedirectHandling().build();
        RestTemplate client = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
        client.setUriTemplateHandler(restTemplate.getRestTemplate().getUriTemplateHandler());
        client.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) throws IOException {
                return false;
            }
        });
        return client;
    }

    private OpenApiIntegrationFixtures.LoginSession loginOn(RestTemplate client, String email, String password) {
        Map<String, String> request = Map.of("email", email, "password", password);
        ResponseEntity<Map> response = client.postForEntity("/api/v1/auth/login", request, Map.class);
        OpenApiIntegrationFixtures.assertApiSuccess(response);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertNotNull(data);
        String token = String.valueOf(data.get("token"));
        Map<?, ?> tenant = (Map<?, ?>) data.get("tenant");
        long tenantId = ((Number) tenant.get("id")).longValue();
        return new OpenApiIntegrationFixtures.LoginSession(token, tenantId);
    }

    private void assertCrossTenantDenied(
            RestTemplate client,
            String attackerToken,
            String path,
            HttpMethod method
    ) {
        ResponseEntity<Map> response = client.exchange(
                path,
                method,
                new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(attackerToken)),
                Map.class
        );
        assertNotNull(response.getBody(), () -> "empty body for " + method + " " + path);
        int code = ((Number) response.getBody().get("code")).intValue();
        String message = String.valueOf(response.getBody().get("message"));
        assertTrue(code != 0, () -> method + " " + path + " should be denied, body=" + response.getBody());
        assertTrue(
                code < 40100 || code >= 40200,
                () -> method + " " + path + " got auth failure instead of tenant deny: code="
                        + code + " message=" + message
        );
    }
}
