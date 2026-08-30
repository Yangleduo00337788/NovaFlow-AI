package ai.novaflow.server.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

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
}
