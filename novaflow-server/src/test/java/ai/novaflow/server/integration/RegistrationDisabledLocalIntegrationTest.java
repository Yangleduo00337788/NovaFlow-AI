package ai.novaflow.server.integration;

import ai.novaflow.user.service.PlatformSystemConfigService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A-03：prod 配置下 registration-enabled=false 时拒绝自助注册。
 */
@Tag("local")
@Execution(ExecutionMode.SAME_THREAD)
class RegistrationDisabledLocalIntegrationTest extends AbstractLocalIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PlatformSystemConfigService platformSystemConfigService;

    @DynamicPropertySource
    static void disableRegistration(DynamicPropertyRegistry registry) {
        registry.add("novaflow.auth.registration-enabled", () -> "false");
    }

    @BeforeEach
    void disableRegistrationInDb() {
        platformSystemConfigService.setRegistrationEnabled(false, null);
    }

    @AfterEach
    void restoreRegistrationInDb() {
        platformSystemConfigService.setRegistrationEnabled(true, null);
    }

    @Test
    void registerRejectedWhenDisabled() {
        String suffix = String.valueOf(System.currentTimeMillis());
        Map<String, Object> body = Map.of(
                "companyName", "QA-Reg-" + suffix,
                "email", "qa-reg-" + suffix + "@novaflow.test",
                "nickname", "QA Reg",
                "password", "SmokeTest123!",
                "confirmPassword", "SmokeTest123!",
                "planType", "enterprise"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/register", body, Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotEquals(0, ((Number) response.getBody().get("code")).intValue());
    }
}
