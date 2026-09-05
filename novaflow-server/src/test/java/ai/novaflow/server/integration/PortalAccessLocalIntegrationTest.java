package ai.novaflow.server.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Z-10：撤销 viewer 角色的 portal:access 后，门户 API 应返回 403。
 */
@Tag("local")
@Execution(ExecutionMode.SAME_THREAD)
class PortalAccessLocalIntegrationTest extends AbstractLocalIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void revokeViewerPortalAccess() {
        PortalAccessTestSupport.revokeViewerPortalAccess(jdbcTemplate);
    }

    @AfterEach
    void restoreViewerPortalAccess() {
        PortalAccessTestSupport.restoreViewerPortalAccess(jdbcTemplate);
    }

    @Test
    void portalRequiresPortalAccessPermission() {
        OpenApiIntegrationFixtures.LoginSession viewer = OpenApiIntegrationFixtures.login(
                restTemplate, "viewer@novaflow.ai", "Viewer123!");

        ResponseEntity<Map> portal = restTemplate.exchange(
                "/api/v1/portal/apps",
                HttpMethod.GET,
                new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(viewer.token())),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiCode(portal, 40301);
    }

    @Test
    void ownerExclusiveTenantApisDeniedForDeveloper() {
        OpenApiIntegrationFixtures.LoginSession developer = OpenApiIntegrationFixtures.login(
                restTemplate, "developer@novaflow.ai", "Developer123!");

        ResponseEntity<Map> deleteTenant = restTemplate.exchange(
                "/api/v1/org/tenant",
                HttpMethod.DELETE,
                new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(developer.token())),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiCode(deleteTenant, 40301);

        ResponseEntity<Map> transfer = restTemplate.exchange(
                "/api/v1/org/tenant/transfer-owner",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("memberId", 1L), OpenApiIntegrationFixtures.adminHeaders(developer.token())),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiCode(transfer, 40301);

        OpenApiIntegrationFixtures.LoginSession owner = OpenApiIntegrationFixtures.login(restTemplate);
        ResponseEntity<Map> tenant = restTemplate.exchange(
                "/api/v1/org/tenant",
                HttpMethod.GET,
                new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(owner.token())),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(tenant);
        assertEquals(0, ((Number) tenant.getBody().get("code")).intValue());
    }
}
