package ai.novaflow.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UrlSafetyValidatorTest {

    @Test
    void allowsPublicHttpsUrl() {
        assertDoesNotThrow(() -> UrlSafetyValidator.validateHttpUrl("https://8.8.8.8/dns"));
    }

    @Test
    void blocksLocalhost() {
        assertThrows(Exception.class, () -> UrlSafetyValidator.validateHttpUrl("http://localhost:8080/admin"));
    }

    @Test
    void blocksPrivateIp() {
        assertThrows(Exception.class, () -> UrlSafetyValidator.validateHttpUrl("http://192.168.1.10/internal"));
    }

    @Test
    void blocksMetadataIp() {
        assertThrows(Exception.class, () -> UrlSafetyValidator.validateHttpUrl("http://169.254.169.254/latest/meta-data"));
    }
}
