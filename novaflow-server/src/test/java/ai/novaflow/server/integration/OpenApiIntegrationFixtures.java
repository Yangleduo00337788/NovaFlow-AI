package ai.novaflow.server.integration;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Open API 集成测试辅助：登录、创建并发布 Agent、构造鉴权头等。
 */
public final class OpenApiIntegrationFixtures {

    private OpenApiIntegrationFixtures() {
    }

    public record LoginSession(String token, long tenantId) {
    }

    public record PublishedAgent(long agentId, long tenantId, String apiKey, String embedToken) {
    }

    public static LoginSession login(TestRestTemplate restTemplate) {
        return login(restTemplate, "admin@novaflow.ai", "Admin123!");
    }

    public static LoginSession login(TestRestTemplate restTemplate, String email, String password) {
        Map<String, String> request = Map.of(
                "email", email,
                "password", password
        );
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/login", request, Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = response.getBody();
        assertNotNull(body);
        assertEquals(0, intCode(body));

        Map<?, ?> data = (Map<?, ?>) body.get("data");
        assertNotNull(data);
        String token = String.valueOf(data.get("token"));
        assertTrue(token.length() > 10);

        Map<?, ?> tenant = (Map<?, ?>) data.get("tenant");
        assertNotNull(tenant);
        long tenantId = ((Number) tenant.get("id")).longValue();
        return new LoginSession(token, tenantId);
    }

    public static LoginSession registerNewTenant(TestRestTemplate restTemplate, String suffix) {
        Map<String, Object> request = new HashMap<>();
        request.put("companyName", "IDOR-" + suffix);
        request.put("email", "idor-" + suffix + "@novaflow.test");
        request.put("nickname", "IDOR " + suffix);
        request.put("password", "SmokeTest123!");
        request.put("confirmPassword", "SmokeTest123!");
        request.put("planType", "enterprise");

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/register", request, Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = response.getBody();
        assertNotNull(body);
        assertEquals(0, intCode(body));

        Map<?, ?> data = (Map<?, ?>) body.get("data");
        assertNotNull(data);
        String token = String.valueOf(data.get("token"));
        Map<?, ?> tenant = (Map<?, ?>) data.get("tenant");
        assertNotNull(tenant);
        long tenantId = ((Number) tenant.get("id")).longValue();
        return new LoginSession(token, tenantId);
    }

    public static long createApplication(TestRestTemplate restTemplate, String token, String appName) {
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("appName", appName);
        createRequest.put("description", "integration test");

        ResponseEntity<Map> createResponse = restTemplate.exchange(
                "/api/v1/applications",
                HttpMethod.POST,
                new HttpEntity<>(createRequest, adminHeaders(token)),
                Map.class
        );
        assertApiSuccess(createResponse);
        Map<?, ?> appData = (Map<?, ?>) createResponse.getBody().get("data");
        assertNotNull(appData);
        return ((Number) appData.get("id")).longValue();
    }

    public static long createChatAgent(TestRestTemplate restTemplate, String token, long applicationId, String agentName) {
        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("agentName", agentName);
        createRequest.put("agentType", "chat");
        createRequest.put("applicationId", applicationId);
        createRequest.put("welcomeMessage", "IDOR test");

        ResponseEntity<Map> createResponse = restTemplate.exchange(
                "/api/v1/agents",
                HttpMethod.POST,
                new HttpEntity<>(createRequest, adminHeaders(token)),
                Map.class
        );
        assertApiSuccess(createResponse);
        Map<?, ?> agentData = (Map<?, ?>) createResponse.getBody().get("data");
        assertNotNull(agentData);
        return ((Number) agentData.get("id")).longValue();
    }

    public static long createKnowledgeBase(TestRestTemplate restTemplate, String token, String kbName) {
        ResponseEntity<Map> optionsResponse = restTemplate.exchange(
                "/api/v1/models/embedding-options",
                HttpMethod.GET,
                new HttpEntity<>(null, adminHeaders(token)),
                Map.class
        );
        assertApiSuccess(optionsResponse);
        List<?> options = (List<?>) optionsResponse.getBody().get("data");
        assertNotNull(options);
        assertTrue(!options.isEmpty(), "embedding options should exist");
        String embeddingModel = String.valueOf(((Map<?, ?>) options.get(0)).get("modelName"));

        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("kbName", kbName);
        createRequest.put("description", "IDOR test");
        createRequest.put("embeddingModel", embeddingModel);

        ResponseEntity<Map> createResponse = restTemplate.exchange(
                "/api/v1/knowledge-bases",
                HttpMethod.POST,
                new HttpEntity<>(createRequest, adminHeaders(token)),
                Map.class
        );
        assertApiSuccess(createResponse);
        Map<?, ?> kbData = (Map<?, ?>) createResponse.getBody().get("data");
        assertNotNull(kbData);
        return ((Number) kbData.get("id")).longValue();
    }

    public static PublishedAgent createAndPublishChatAgent(TestRestTemplate restTemplate) {
        LoginSession session = login(restTemplate);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long applicationId = createApplication(restTemplate, session.token(), "E2E-OpenAPI-App-" + suffix);

        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("agentName", "E2E-OpenAPI-" + suffix);
        createRequest.put("agentType", "chat");
        createRequest.put("applicationId", applicationId);
        createRequest.put("welcomeMessage", "E2E welcome");

        ResponseEntity<Map> createResponse = restTemplate.exchange(
                "/api/v1/agents",
                HttpMethod.POST,
                new HttpEntity<>(createRequest, adminHeaders(session.token())),
                Map.class
        );
        assertEquals(HttpStatus.OK, createResponse.getStatusCode());
        Map<?, ?> createBody = createResponse.getBody();
        assertNotNull(createBody);
        assertEquals(0, intCode(createBody));
        Map<?, ?> agentData = (Map<?, ?>) createBody.get("data");
        assertNotNull(agentData);
        long agentId = ((Number) agentData.get("id")).longValue();

        ResponseEntity<Map> publishResponse = restTemplate.exchange(
                "/api/v1/agents/" + agentId + "/publish",
                HttpMethod.POST,
                new HttpEntity<>(null, adminHeaders(session.token())),
                Map.class
        );
        assertEquals(HttpStatus.OK, publishResponse.getStatusCode());
        Map<?, ?> publishBody = publishResponse.getBody();
        assertNotNull(publishBody);
        assertEquals(0, intCode(publishBody));
        Map<?, ?> publishData = (Map<?, ?>) publishBody.get("data");
        assertNotNull(publishData);

        Map<String, Object> updateApp = new HashMap<>();
        updateApp.put("appName", "E2E-OpenAPI-App-" + suffix);
        updateApp.put("description", "open api fixture");
        updateApp.put("defaultAgentId", agentId);
        updateApp.put("agentIds", List.of(agentId));
        ResponseEntity<Map> appUpdated = restTemplate.exchange(
                "/api/v1/applications/" + applicationId,
                HttpMethod.PUT,
                new HttpEntity<>(updateApp, adminHeaders(session.token())),
                Map.class
        );
        assertApiSuccess(appUpdated);

        ResponseEntity<Map> appPublished = restTemplate.exchange(
                "/api/v1/applications/" + applicationId + "/publish",
                HttpMethod.POST,
                new HttpEntity<>(null, adminHeaders(session.token())),
                Map.class
        );
        assertApiSuccess(appPublished);

        String apiKey = String.valueOf(publishData.get("apiKey"));
        String embedToken = String.valueOf(publishData.get("embedToken"));
        assertTrue(apiKey.startsWith("nf_live_"), "apiKey should use nf_live_ prefix");
        assertTrue(embedToken.startsWith("nf_embed_"), "embedToken should use nf_embed_ prefix");

        return new PublishedAgent(agentId, session.tenantId(), apiKey, embedToken);
    }

    public static HttpHeaders adminHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token);
        return headers;
    }

    public static HttpHeaders apiKeyHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    public static HttpHeaders embedTokenHeaders(String embedToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Embed-Token", embedToken);
        return headers;
    }

    public static HttpHeaders embedTokenHeaders(String embedToken, String callerId) {
        HttpHeaders headers = embedTokenHeaders(embedToken);
        headers.set("X-Caller-Id", callerId);
        return headers;
    }

    public static ResponseEntity<Map> getOpenApi(
            TestRestTemplate restTemplate,
            String path,
            HttpHeaders headers) {
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(null, headers), Map.class);
    }

    public static void assertApiCode(ResponseEntity<Map> response, int expectedCode) {
        Map<?, ?> body = response.getBody();
        assertNotNull(body);
        int actualCode = intCode(body);
        assertEquals(expectedCode, actualCode, () -> "message=" + body.get("message"));

        HttpStatus expectedStatus = expectedCode == 0 ? HttpStatus.OK : httpStatusOf(expectedCode);
        assertEquals(
                expectedStatus,
                response.getStatusCode(),
                () -> "body.code=" + actualCode + ", http=" + response.getStatusCode()
        );
    }

    static HttpStatus httpStatusOf(int code) {
        if (code >= 40100 && code < 40200) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code >= 40300 && code < 40400) {
            return HttpStatus.FORBIDDEN;
        }
        if (code >= 42900 && code < 43000) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        if (code >= 50000 && code < 60000) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.BAD_REQUEST;
    }

    public static void assertApiSuccess(ResponseEntity<Map> response) {
        assertApiCode(response, 0);
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> extractConversationList(ResponseEntity<Map> response) {
        assertApiSuccess(response);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertNotNull(data);
        Object list = data.get("list");
        assertNotNull(list);
        return (List<Map<String, Object>>) list;
    }

    private static long firstApplicationId(TestRestTemplate restTemplate, String token) {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/applications/options",
                HttpMethod.GET,
                new HttpEntity<>(null, adminHeaders(token)),
                Map.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = response.getBody();
        assertNotNull(body);
        assertEquals(0, intCode(body));
        List<?> options = (List<?>) body.get("data");
        assertNotNull(options);
        assertTrue(!options.isEmpty(), "demo application should exist after DataInitializer");
        Map<?, ?> first = (Map<?, ?>) options.get(0);
        return ((Number) first.get("id")).longValue();
    }

    private static int intCode(Map<?, ?> body) {
        return ((Number) body.get("code")).intValue();
    }
}
