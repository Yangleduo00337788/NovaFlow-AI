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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 平台超管 API 冒烟（使用 account_type=platform 专用账号）。
 */
@Tag("local")
@Execution(ExecutionMode.SAME_THREAD)
class PlatformAdminLocalIntegrationTest extends AbstractLocalIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void platformTenantsAndStats() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.loginPlatform(restTemplate);
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

        ResponseEntity<Map> dashboard = restTemplate.exchange(
                "/api/v1/platform/dashboard/overview",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(dashboard);
        Map<?, ?> dashboardData = (Map<?, ?>) dashboard.getBody().get("data");
        org.junit.jupiter.api.Assertions.assertNotNull(dashboardData.get("stats"));
        org.junit.jupiter.api.Assertions.assertNotNull(dashboardData.get("tenantGrowthTrend"));
        org.junit.jupiter.api.Assertions.assertNotNull(dashboardData.get("tokenUsageTrend"));
        org.junit.jupiter.api.Assertions.assertNotNull(dashboardData.get("tenantHealth"));

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
    void platformAuditLogsQuery() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.loginPlatform(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        ResponseEntity<Map> logs = restTemplate.exchange(
                "/api/v1/platform/audit-logs?page=1&pageSize=5",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(logs);
    }

    @Test
    void platformAccountBlockedFromTenantAuditApi() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.loginPlatform(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        ResponseEntity<Map> logs = restTemplate.exchange(
                "/api/v1/audit-logs?page=1&pageSize=5",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        assertEquals(HttpStatus.FORBIDDEN, logs.getStatusCode());
    }

    @Test
    void tenantAccountBlockedFromPlatformAuditApi() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.login(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        ResponseEntity<Map> logs = restTemplate.exchange(
                "/api/v1/platform/audit-logs?page=1&pageSize=5",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        assertEquals(HttpStatus.FORBIDDEN, logs.getStatusCode());
    }

    @Test
    void platformUsersAndLoginLogs() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.loginPlatform(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        ResponseEntity<Map> users = restTemplate.exchange(
                "/api/v1/platform/users?page=1&pageSize=10",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(users);

        ResponseEntity<Map> loginLogs = restTemplate.exchange(
                "/api/v1/platform/login-logs?page=1&pageSize=10",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(loginLogs);

        Map<?, ?> userData = (Map<?, ?>) users.getBody().get("data");
        assert userData != null;
        List<?> userList = (List<?>) userData.get("list");
        assert userList != null && !userList.isEmpty();

        long targetUserId = findUserIdByEmail(userList, "developer@novaflow.ai");
        ResponseEntity<Map> detail = restTemplate.exchange(
                "/api/v1/platform/users/" + targetUserId,
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(detail);

        Map<String, Object> banBody = Map.of("status", 0);
        ResponseEntity<Map> ban = restTemplate.exchange(
                "/api/v1/platform/users/" + targetUserId,
                HttpMethod.PUT,
                new HttpEntity<>(banBody, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(ban);

        ResponseEntity<Map> logout = restTemplate.exchange(
                "/api/v1/platform/users/" + targetUserId + "/logout",
                HttpMethod.POST,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(logout);

        Map<String, Object> unbanBody = Map.of("status", 1);
        ResponseEntity<Map> unban = restTemplate.exchange(
                "/api/v1/platform/users/" + targetUserId,
                HttpMethod.PUT,
                new HttpEntity<>(unbanBody, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(unban);
    }

    @Test
    void platformBillingAndModelOverview() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.loginPlatform(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        ResponseEntity<Map> billing = restTemplate.exchange(
                "/api/v1/platform/billing/overview",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(billing);

        ResponseEntity<Map> models = restTemplate.exchange(
                "/api/v1/platform/models/overview",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(models);

        ResponseEntity<Map> providerPage = restTemplate.exchange(
                "/api/v1/platform/models/providers?page=1&pageSize=10",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(providerPage);

        Map<?, ?> providerData = (Map<?, ?>) providerPage.getBody().get("data");
        List<?> providerList = (List<?>) providerData.get("list");
        if (providerList != null && !providerList.isEmpty()) {
            long providerId = ((Number) ((Map<?, ?>) providerList.get(0)).get("id")).longValue();
            Map<String, Object> updateBody = Map.of("enabled", 1);
            ResponseEntity<Map> updateProvider = restTemplate.exchange(
                    "/api/v1/platform/models/providers/" + providerId,
                    HttpMethod.PUT,
                    new HttpEntity<>(updateBody, headers),
                    Map.class
            );
            OpenApiIntegrationFixtures.assertApiSuccess(updateProvider);
        }
    }

    @Test
    void platformIpBlacklistCrud() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.loginPlatform(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        Map<String, Object> createBody = Map.of(
                "ipAddress", "203.0.113.99",
                "reason", "integration-test"
        );
        ResponseEntity<Map> create = restTemplate.exchange(
                "/api/v1/platform/ip-blacklist",
                HttpMethod.POST,
                new HttpEntity<>(createBody, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(create);

        Map<?, ?> created = (Map<?, ?>) ((Map<?, ?>) create.getBody().get("data"));
        long id = ((Number) created.get("id")).longValue();

        ResponseEntity<Map> list = restTemplate.exchange(
                "/api/v1/platform/ip-blacklist?page=1&pageSize=10&keyword=203.0.113.99",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(list);

        Map<String, Object> updateBody = Map.of("status", 0, "reason", "disabled");
        ResponseEntity<Map> update = restTemplate.exchange(
                "/api/v1/platform/ip-blacklist/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(updateBody, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(update);

        ResponseEntity<Map> delete = restTemplate.exchange(
                "/api/v1/platform/ip-blacklist/" + id,
                HttpMethod.DELETE,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(delete);
    }

    @Test
    void platformTenantCreateSettingsAndUserDelete() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.loginPlatform(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        String uniqueEmail = "phase20-" + System.currentTimeMillis() + "@novaflow.test";
        Map<String, Object> createBody = Map.of(
                "tenantName", "Phase20 Test Corp",
                "planType", "free",
                "ownerEmail", uniqueEmail,
                "ownerPassword", "Test1234"
        );
        ResponseEntity<Map> create = restTemplate.exchange(
                "/api/v1/platform/tenants",
                HttpMethod.POST,
                new HttpEntity<>(createBody, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(create);

        Map<?, ?> createData = (Map<?, ?>) create.getBody().get("data");
        Map<?, ?> createdTenant = (Map<?, ?>) createData.get("tenant");
        long tenantId = ((Number) createdTenant.get("id")).longValue();

        ResponseEntity<Map> users = restTemplate.exchange(
                "/api/v1/platform/users?page=1&pageSize=50&keyword=" + uniqueEmail,
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(users);
        Map<?, ?> userData = (Map<?, ?>) users.getBody().get("data");
        List<?> userList = (List<?>) userData.get("list");
        long targetUserId = findUserIdByEmail(userList, uniqueEmail);

        ResponseEntity<Map> deleteUser = restTemplate.exchange(
                "/api/v1/platform/users/" + targetUserId,
                HttpMethod.DELETE,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(deleteUser);

        ResponseEntity<Map> deleteTenant = restTemplate.exchange(
                "/api/v1/platform/tenants/" + tenantId,
                HttpMethod.DELETE,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(deleteTenant);

        ResponseEntity<Map> settings = restTemplate.exchange(
                "/api/v1/platform/settings",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(settings);

        Map<String, Object> updateSettings = Map.of("registrationEnabled", true);
        ResponseEntity<Map> update = restTemplate.exchange(
                "/api/v1/platform/settings",
                HttpMethod.PUT,
                new HttpEntity<>(updateSettings, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(update);
    }

    @Test
    void platformApiMonitorAndSettings() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.loginPlatform(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        ResponseEntity<Map> monitor = restTemplate.exchange(
                "/api/v1/platform/api-monitor/overview",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(monitor);

        ResponseEntity<Map> settings = restTemplate.exchange(
                "/api/v1/platform/settings",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(settings);

        Map<String, Object> updateBody = Map.of(
                "hourlyCallsThreshold", 500,
                "trafficSpikeMultiplier", 3,
                "allowedProviderCodes", List.of("openai", "deepseek")
        );
        ResponseEntity<Map> update = restTemplate.exchange(
                "/api/v1/platform/settings",
                HttpMethod.PUT,
                new HttpEntity<>(updateBody, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(update);

        Map<String, Object> resetBody = Map.of("allowedProviderCodes", List.of());
        ResponseEntity<Map> reset = restTemplate.exchange(
                "/api/v1/platform/settings",
                HttpMethod.PUT,
                new HttpEntity<>(resetBody, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(reset);
    }

    @Test
    void platformTenantDetailAndQuotaUpdate() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.loginPlatform(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        String uniqueEmail = "phase25-" + System.currentTimeMillis() + "@novaflow.test";
        Map<String, Object> createBody = Map.of(
                "tenantName", "Phase25 Detail Corp",
                "planType", "pro",
                "ownerEmail", uniqueEmail,
                "ownerPassword", "Test1234"
        );
        ResponseEntity<Map> create = restTemplate.exchange(
                "/api/v1/platform/tenants",
                HttpMethod.POST,
                new HttpEntity<>(createBody, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(create);

        Map<?, ?> createData = (Map<?, ?>) create.getBody().get("data");
        Map<?, ?> createdTenant = (Map<?, ?>) createData.get("tenant");
        long tenantId = ((Number) createdTenant.get("id")).longValue();

        Map<String, Object> updateBody = Map.of(
                "tenantName", "Phase25 Updated Corp",
                "planType", "enterprise",
                "status", 1,
                "expireAt", "2030-12-31T23:59:59",
                "maxMembers", 120,
                "maxAgents", 80,
                "maxKnowledge", 40,
                "maxStorageMb", 20480,
                "monthlyTokenQuota", 5000000
        );
        ResponseEntity<Map> update = restTemplate.exchange(
                "/api/v1/platform/tenants/" + tenantId,
                HttpMethod.PUT,
                new HttpEntity<>(updateBody, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(update);

        Map<?, ?> updated = (Map<?, ?>) update.getBody().get("data");
        org.junit.jupiter.api.Assertions.assertEquals("Phase25 Updated Corp", updated.get("tenantName"));
        org.junit.jupiter.api.Assertions.assertEquals(120, ((Number) updated.get("maxMembers")).intValue());
        org.junit.jupiter.api.Assertions.assertEquals(20480, ((Number) updated.get("maxStorageMb")).intValue());
        org.junit.jupiter.api.Assertions.assertEquals(5000000L, ((Number) updated.get("monthlyTokenQuota")).longValue());

        ResponseEntity<Map> detail = restTemplate.exchange(
                "/api/v1/platform/tenants/" + tenantId + "/detail",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(detail);

        Map<?, ?> detailData = (Map<?, ?>) detail.getBody().get("data");
        Map<?, ?> tenant = (Map<?, ?>) detailData.get("tenant");
        org.junit.jupiter.api.Assertions.assertEquals("Phase25 Updated Corp", tenant.get("tenantName"));
        org.junit.jupiter.api.Assertions.assertEquals(120, ((Number) tenant.get("maxMembers")).intValue());
        org.junit.jupiter.api.Assertions.assertNotNull(detailData.get("dailyTokenTrend"));
        org.junit.jupiter.api.Assertions.assertNotNull(detailData.get("topModelsThisMonth"));

        ResponseEntity<Map> deleteTenant = restTemplate.exchange(
                "/api/v1/platform/tenants/" + tenantId,
                HttpMethod.DELETE,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(deleteTenant);
    }

    @Test
    void platformBillingExportCsv() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.loginPlatform(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        ResponseEntity<byte[]> export = restTemplate.exchange(
                "/api/v1/platform/billing/export",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                byte[].class
        );
        assertEquals(HttpStatus.OK, export.getStatusCode());
        assertNotNull(export.getBody());
        String csv = new String(export.getBody(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("section,field,value"));
        assertTrue(csv.contains("tenantId,tenantName,calls,tokens"));
    }

    @Test
    void platformApiAlertHistoryAndAck() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.loginPlatform(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        ResponseEntity<Map> monitor = restTemplate.exchange(
                "/api/v1/platform/api-monitor/overview",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(monitor);

        ResponseEntity<Map> alerts = restTemplate.exchange(
                "/api/v1/platform/api-monitor/alerts?page=1&pageSize=20",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(alerts);

        Map<?, ?> alertData = (Map<?, ?>) alerts.getBody().get("data");
        List<?> alertList = (List<?>) alertData.get("list");
        if (alertList != null && !alertList.isEmpty()) {
            long alertId = ((Number) ((Map<?, ?>) alertList.get(0)).get("id")).longValue();
            String status = String.valueOf(((Map<?, ?>) alertList.get(0)).get("status"));
            if (!"ACKED".equalsIgnoreCase(status)) {
                ResponseEntity<Map> ack = restTemplate.exchange(
                        "/api/v1/platform/api-monitor/alerts/" + alertId + "/ack",
                        HttpMethod.POST,
                        new HttpEntity<>(null, headers),
                        Map.class
                );
                OpenApiIntegrationFixtures.assertApiSuccess(ack);
                Map<?, ?> acked = (Map<?, ?>) ack.getBody().get("data");
                assertEquals("ACKED", acked.get("status"));
            }
        }
    }

    @Test
    void platformModelCatalogCrud() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.loginPlatform(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        ResponseEntity<Map> list = restTemplate.exchange(
                "/api/v1/platform/models/catalog?page=1&pageSize=10",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(list);

        String modelName = "catalog-test-" + System.currentTimeMillis();
        Map<String, Object> createBody = Map.of(
                "providerCode", "deepseek",
                "modelName", modelName,
                "displayName", "Catalog Test",
                "inputPricePer1k", 0.001,
                "outputPricePer1k", 0.002,
                "currency", "CNY",
                "enabled", 1,
                "description", "integration test"
        );
        ResponseEntity<Map> create = restTemplate.exchange(
                "/api/v1/platform/models/catalog",
                HttpMethod.POST,
                new HttpEntity<>(createBody, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(create);
        long catalogId = ((Number) ((Map<?, ?>) create.getBody().get("data")).get("id")).longValue();

        Map<String, Object> updateBody = Map.of(
                "providerCode", "deepseek",
                "modelName", modelName,
                "displayName", "Catalog Test Updated",
                "inputPricePer1k", 0.002,
                "outputPricePer1k", 0.003,
                "currency", "CNY",
                "enabled", 0,
                "description", "updated"
        );
        ResponseEntity<Map> update = restTemplate.exchange(
                "/api/v1/platform/models/catalog/" + catalogId,
                HttpMethod.PUT,
                new HttpEntity<>(updateBody, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(update);

        ResponseEntity<Map> delete = restTemplate.exchange(
                "/api/v1/platform/models/catalog/" + catalogId,
                HttpMethod.DELETE,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(delete);
    }

    @Test
    void platformSettingsMaintenanceFields() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.loginPlatform(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        Map<String, Object> updateBody = Map.of(
                "maintenanceEnabled", true,
                "maintenanceMessage", "integration test maintenance",
                "platformAnnouncement", "integration test announcement"
        );
        ResponseEntity<Map> update = restTemplate.exchange(
                "/api/v1/platform/settings",
                HttpMethod.PUT,
                new HttpEntity<>(updateBody, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(update);

        ResponseEntity<Map> settings = restTemplate.exchange(
                "/api/v1/platform/settings",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(settings);
        Map<?, ?> data = (Map<?, ?>) settings.getBody().get("data");
        assertEquals(true, data.get("maintenanceEnabled"));
        assertEquals("integration test maintenance", data.get("maintenanceMessage"));
        assertEquals("integration test announcement", data.get("platformAnnouncement"));

        Map<String, Object> resetBody = Map.of(
                "maintenanceEnabled", false,
                "maintenanceMessage", "",
                "platformAnnouncement", ""
        );
        ResponseEntity<Map> reset = restTemplate.exchange(
                "/api/v1/platform/settings",
                HttpMethod.PUT,
                new HttpEntity<>(resetBody, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(reset);
    }

    @Test
    void platformAuditorAccessControl() {
        OpenApiIntegrationFixtures.LoginSession auditor = OpenApiIntegrationFixtures.login(
                restTemplate, "auditor@novaflow.ai", "Auditor123!");
        var headers = OpenApiIntegrationFixtures.adminHeaders(auditor.token());

        ResponseEntity<Map> auditLogs = restTemplate.exchange(
                "/api/v1/platform/audit-logs?page=1&pageSize=5",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(auditLogs);

        ResponseEntity<Map> tenants = restTemplate.exchange(
                "/api/v1/platform/tenants?page=1&pageSize=5",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        assertEquals(HttpStatus.FORBIDDEN, tenants.getStatusCode());

        ResponseEntity<Map> settings = restTemplate.exchange(
                "/api/v1/platform/settings",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        assertEquals(HttpStatus.FORBIDDEN, settings.getStatusCode());
    }

    private long findUserIdByEmail(List<?> userList, String email) {
        for (Object item : userList) {
            Map<?, ?> user = (Map<?, ?>) item;
            if (email.equals(user.get("email"))) {
                return ((Number) user.get("id")).longValue();
            }
        }
        throw new IllegalStateException("User not found: " + email);
    }

    @Test
    void platformTenantOnboardingEnhancement() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.loginPlatform(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());

        ResponseEntity<Map> templates = restTemplate.exchange(
                "/api/v1/platform/onboarding/templates",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(templates);
        List<?> templateList = (List<?>) templates.getBody().get("data");
        org.junit.jupiter.api.Assertions.assertFalse(templateList.isEmpty());

        String uniqueEmail = "phase31-" + System.currentTimeMillis() + "@novaflow.test";
        Map<String, Object> createBody = new java.util.HashMap<>();
        createBody.put("tenantName", "Phase31 Onboarding Corp");
        createBody.put("planType", "starter");
        createBody.put("ownerEmail", uniqueEmail);
        createBody.put("generatePassword", true);
        createBody.put("sendInviteEmail", false);

        ResponseEntity<Map> create = restTemplate.exchange(
                "/api/v1/platform/tenants",
                HttpMethod.POST,
                new HttpEntity<>(createBody, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(create);
        Map<?, ?> createData = (Map<?, ?>) create.getBody().get("data");
        org.junit.jupiter.api.Assertions.assertNotNull(createData.get("generatedPassword"));
        long tenantId = ((Number) ((Map<?, ?>) createData.get("tenant")).get("id")).longValue();
        Object initialPassword = createData.get("generatedPassword");

        Map<String, Object> resetBody = Map.of(
                "generatePassword", true,
                "sendInviteEmail", false
        );
        ResponseEntity<Map> reset = restTemplate.exchange(
                "/api/v1/platform/tenants/" + tenantId + "/owner/reset-password",
                HttpMethod.POST,
                new HttpEntity<>(resetBody, headers),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(reset);
        Map<?, ?> resetData = (Map<?, ?>) reset.getBody().get("data");
        org.junit.jupiter.api.Assertions.assertNotNull(resetData.get("generatedPassword"));

        ResponseEntity<Map> initialLogin = restTemplate.exchange(
                "/api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("email", uniqueEmail, "password", initialPassword), null),
                Map.class
        );
        org.junit.jupiter.api.Assertions.assertNotEquals(0, initialLogin.getBody().get("code"));

        ResponseEntity<Map> ownerLogin = restTemplate.exchange(
                "/api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "email", uniqueEmail,
                        "password", resetData.get("generatedPassword")
                ), null),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(ownerLogin);

        OpenApiIntegrationFixtures.LoginSession platformAgain = OpenApiIntegrationFixtures.loginPlatform(restTemplate);
        headers = OpenApiIntegrationFixtures.adminHeaders(platformAgain.token());

        long targetUserId = ((Number) createData.get("ownerId")).longValue();
        restTemplate.exchange(
                "/api/v1/platform/users/" + targetUserId,
                HttpMethod.DELETE,
                new HttpEntity<>(null, headers),
                Map.class
        );
        restTemplate.exchange(
                "/api/v1/platform/tenants/" + tenantId,
                HttpMethod.DELETE,
                new HttpEntity<>(null, headers),
                Map.class
        );
    }

    @Test
    void platformMaintenanceTenantEnforcement() {
        OpenApiIntegrationFixtures.LoginSession platform = OpenApiIntegrationFixtures.loginPlatform(restTemplate);
        var platformHeaders = OpenApiIntegrationFixtures.adminHeaders(platform.token());

        ResponseEntity<Map> publicStatus = restTemplate.exchange(
                "/api/v1/public/platform-status",
                HttpMethod.GET,
                new HttpEntity<>(null, null),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(publicStatus);

        ResponseEntity<Map> enable = restTemplate.exchange(
                "/api/v1/platform/settings",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of(
                        "maintenanceEnabled", true,
                        "maintenanceMessage", "Phase34 maintenance test",
                        "platformAnnouncement", "Phase34 announcement"
                ), platformHeaders),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(enable);

        ResponseEntity<Map> publicAfter = restTemplate.exchange(
                "/api/v1/public/platform-status",
                HttpMethod.GET,
                new HttpEntity<>(null, null),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(publicAfter);
        Map<?, ?> statusData = (Map<?, ?>) publicAfter.getBody().get("data");
        org.junit.jupiter.api.Assertions.assertEquals(true, statusData.get("maintenanceEnabled"));

        ResponseEntity<Map> tenantLogin = restTemplate.exchange(
                "/api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "email", "admin@novaflow.ai",
                        "password", "Admin123!"
                ), null),
                Map.class
        );
        org.junit.jupiter.api.Assertions.assertNotEquals(0, tenantLogin.getBody().get("code"));

        ResponseEntity<Map> platformRelogin = restTemplate.exchange(
                "/api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "email", "platform@novaflow.ai",
                        "password", "Platform123!"
                ), null),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(platformRelogin);

        restTemplate.exchange(
                "/api/v1/platform/settings",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of(
                        "maintenanceEnabled", false,
                        "maintenanceMessage", "",
                        "platformAnnouncement", ""
                ), platformHeaders),
                Map.class
        );
    }
}
