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
        Map<String, String> request = Map.of(
                "email", "admin@novaflow.ai",
                "password", "Admin123!"
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

    public static PublishedAgent createAndPublishChatAgent(TestRestTemplate restTemplate) {
        LoginSession session = login(restTemplate);
        long applicationId = firstApplicationId(restTemplate, session.token());

        Map<String, Object> createRequest = new HashMap<>();
        createRequest.put("agentName", "E2E-OpenAPI-" + UUID.randomUUID().toString().substring(0, 8));
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
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = response.getBody();
        assertNotNull(body);
        assertEquals(expectedCode, intCode(body), () -> "message=" + body.get("message"));
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
