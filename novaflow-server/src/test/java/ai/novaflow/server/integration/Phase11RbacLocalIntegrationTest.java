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
 * Phase 11：自定义角色 CRUD、可分配列表、平台/租户账号 API 域隔离。
 */
@Tag("local")
@Execution(ExecutionMode.SAME_THREAD)
class Phase11RbacLocalIntegrationTest extends AbstractLocalIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void platformLoginUsesPlatformAccountAndTenantApisBlocked() {
        Map<String, String> request = Map.of(
                "email", "platform@novaflow.ai",
                "password", "Platform123!"
        );
        ResponseEntity<Map> login = restTemplate.postForEntity("/api/v1/auth/login", request, Map.class);
        assertEquals(HttpStatus.OK, login.getStatusCode());
        Map<?, ?> body = login.getBody();
        assertNotNull(body);
        assertEquals(0, ((Number) body.get("code")).intValue());

        Map<?, ?> data = (Map<?, ?>) body.get("data");
        Map<?, ?> user = (Map<?, ?>) data.get("user");
        Map<?, ?> tenant = (Map<?, ?>) data.get("tenant");
        assertEquals("platform", user.get("accountType"));
        assertEquals(0L, ((Number) tenant.get("id")).longValue());

        String token = String.valueOf(data.get("token"));
        ResponseEntity<Map> agents = restTemplate.exchange(
                "/api/v1/agents?page=1&pageSize=5",
                HttpMethod.GET,
                new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(token)),
                Map.class
        );
        assertEquals(HttpStatus.FORBIDDEN, agents.getStatusCode());

        ResponseEntity<Map> tenants = restTemplate.exchange(
                "/api/v1/platform/tenants?page=1&pageSize=5",
                HttpMethod.GET,
                new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(token)),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(tenants);
    }

    @Test
    void customRoleCrudAssignabilityAndBoundaries() {
        OpenApiIntegrationFixtures.LoginSession admin = OpenApiIntegrationFixtures.login(restTemplate);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String roleName = "QA-Custom-" + suffix;

        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("roleName", roleName);
        createRequest.put("description", "phase11 integration");
        createRequest.put("permissionCodes", List.of("agent:read", "knowledge:read"));

        ResponseEntity<Map> created = restTemplate.exchange(
                "/api/v1/roles",
                HttpMethod.POST,
                new HttpEntity<>(createRequest, OpenApiIntegrationFixtures.adminHeaders(admin.token())),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(created);
        Map<?, ?> roleData = (Map<?, ?>) created.getBody().get("data");
        assertNotNull(roleData);
        long roleId = ((Number) roleData.get("id")).longValue();
        String roleCode = String.valueOf(roleData.get("roleCode"));
        assertTrue(roleCode.startsWith("custom_"));

        ResponseEntity<Map> assignable = restTemplate.exchange(
                "/api/v1/roles/assignable",
                HttpMethod.GET,
                new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(admin.token())),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(assignable);
        List<?> assignableList = (List<?>) assignable.getBody().get("data");
        assertNotNull(assignableList);
        assertTrue(assignableList.stream().anyMatch(item -> roleCode.equals(((Map<?, ?>) item).get("roleCode"))));

        Map<String, Object> forbiddenRequest = new HashMap<>();
        forbiddenRequest.put("roleName", "QA-Forbidden-" + suffix);
        forbiddenRequest.put("permissionCodes", List.of("tenant:delete"));
        ResponseEntity<Map> forbidden = restTemplate.exchange(
                "/api/v1/roles",
                HttpMethod.POST,
                new HttpEntity<>(forbiddenRequest, OpenApiIntegrationFixtures.adminHeaders(admin.token())),
                Map.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, forbidden.getStatusCode());
        assertNotNull(forbidden.getBody());
        assertTrue(((Number) forbidden.getBody().get("code")).intValue() != 0);

        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("roleName", roleName + "-Updated");
        updateRequest.put("description", "updated");
        updateRequest.put("permissionCodes", List.of("agent:read"));
        ResponseEntity<Map> updated = restTemplate.exchange(
                "/api/v1/roles/" + roleId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, OpenApiIntegrationFixtures.adminHeaders(admin.token())),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(updated);

        ResponseEntity<Map> deleted = restTemplate.exchange(
                "/api/v1/roles/" + roleId,
                HttpMethod.DELETE,
                new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(admin.token())),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(deleted);
    }
}
