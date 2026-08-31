package ai.novaflow.server.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("local")
class AuthLocalIntegrationTest extends AbstractLocalIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthEndpointIsUp() {
        assertHealthUp(restTemplate);
    }

    @Test
    void loginWithDemoAccountReturnsToken() {
        assertLoginSuccess(restTemplate);
    }

    @Test
    void loginThenFetchApplicationOptions() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.login(restTemplate);
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/applications/options",
                HttpMethod.GET,
                new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(session.token())),
                Map.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
