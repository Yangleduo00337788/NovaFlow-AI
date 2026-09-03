package ai.novaflow.billing.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlertDispatchServiceTest {

    @Test
    void hmacSha256IsStable() throws Exception {
        String hex = AlertDispatchService.hmacSha256("secret", "{\"ok\":true}");
        assertEquals(64, hex.length());
        assertEquals(hex, AlertDispatchService.hmacSha256("secret", "{\"ok\":true}"));
    }
}
