package ai.novaflow.server.integration;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class IntegrationTestSupport {

    protected void assertHealthUp(TestRestTemplate restTemplate) {
        ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/health", Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
    }

    protected void assertLoginSuccess(TestRestTemplate restTemplate) {
        Map<String, String> request = Map.of(
                "email", "admin@novaflow.ai",
                "password", "Admin123!"
        );
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/login", request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, ((Number) response.getBody().get("code")).intValue());

        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertNotNull(data);
        assertTrue(String.valueOf(data.get("token")).length() > 10);
    }
}
