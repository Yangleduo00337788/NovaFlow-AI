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

/**
 * 平台超管与审计日志 API 冒烟（临时提升演示账号为 super_admin）。
 */
@Tag("local")
@Execution(ExecutionMode.SAME_THREAD)
class PlatformAdminLocalIntegrationTest extends AbstractLocalIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void promoteSuperAdmin() {
        SuperAdminTestSupport.promoteDemoUserToSuperAdmin(jdbcTemplate);
    }

    @AfterEach
    void restoreTenantAdmin() {
        SuperAdminTestSupport.restoreDemoUserTenantAdmin(jdbcTemplate);
    }

    @Test
    void platformTenantsAndStats() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.login(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        ResponseEntity<Map> tenants = restTemplate.exchange(
                "/api/v1/platform/tenants?page=1&pageSize=5",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(tenants);

        ResponseEntity<Map> stats = restTemplate.exchange(
                "/api/v1/platform/stats",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(stats);

        Map<?, ?> data = (Map<?, ?>) tenants.getBody().get("data");
        if (data != null && data.get("list") instanceof java.util.List<?> list && !list.isEmpty()) {
            long tenantId = ((Number) ((Map<?, ?>) list.get(0)).get("id")).longValue();
            ResponseEntity<Map> detail = restTemplate.exchange(
                    "/api/v1/platform/tenants/" + tenantId,
                    HttpMethod.GET,
                    new HttpEntity<>(null, headers),
                    Map.class
            );
            OpenApiIntegrationFixtures.assertApiSuccess(detail);
        }
    }

    @Test
    void auditLogsQuery() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.login(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        ResponseEntity<Map> logs = restTemplate.exchange(
                "/api/v1/audit-logs?page=1&pageSize=5",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(logs);
    }
}
