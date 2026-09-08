package ai.novaflow.server.integration;

import ai.novaflow.user.service.PlatformSystemConfigService;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class IntegrationTestSupport {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired(required = false)
    private PlatformSystemConfigService platformSystemConfigService;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /**
     * JDK HttpURLConnection 会在 GET 请求中静默丢弃 Authorization 头。
     */
    @BeforeEach
    void configureIntegrationRestTemplate() {
        var httpClient = HttpClients.custom().disableRedirectHandling().build();
        var requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        var raw = restTemplate.getRestTemplate();
        raw.setRequestFactory(requestFactory);
        // 保留 4xx/5xx 响应体与状态码，供 assertApiCode 校验（默认 ErrorHandler 会直接抛异常）
        raw.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return false;
            }
        });
        prepareSharedGateState();
    }

    private void prepareSharedGateState() {
        if (platformSystemConfigService != null) {
            platformSystemConfigService.setRegistrationEnabled(true, null);
            platformSystemConfigService.setBatchRegisterIpLimitPerDay(100_000, null);
            platformSystemConfigService.setMaintenanceEnabled(false, null);
            platformSystemConfigService.setMaintenanceMessage("", null);
            platformSystemConfigService.setPlatformAnnouncement("", null);
        }
        if (stringRedisTemplate != null) {
            Set<String> keys = stringRedisTemplate.keys("novaflow:risk:register:ip:*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        }
    }

    @AfterEach
    void resetSharedGateState() {
        prepareSharedGateState();
    }

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
