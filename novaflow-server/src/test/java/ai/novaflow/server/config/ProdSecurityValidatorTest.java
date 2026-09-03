package ai.novaflow.server.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProdSecurityValidatorTest {

    @Test
    void rejectsEmptyWildcardAndLocalhostInProd() {
        assertThrows(IllegalStateException.class, () -> ProdSecurityValidator.validateCorsOrigins(List.of(), false));
        assertThrows(IllegalStateException.class, () -> ProdSecurityValidator.validateCorsOrigins(List.of("*"), false));
        assertThrows(IllegalStateException.class, () ->
                ProdSecurityValidator.validateCorsOrigins(List.of("http://localhost:3000"), false));
        assertThrows(IllegalStateException.class, () ->
                ProdSecurityValidator.validateCorsOrigins(List.of("http://127.0.0.1:8080"), false));
    }

    @Test
    void allowsRealOriginAndLocalhostEscapeHatch() {
        assertDoesNotThrow(() ->
                ProdSecurityValidator.validateCorsOrigins(List.of("https://app.example.com"), false));
        assertDoesNotThrow(() ->
                ProdSecurityValidator.validateCorsOrigins(List.of("http://localhost:13000"), true));
        assertTrue(ProdSecurityValidator.isLocalOrigin("http://localhost:3000"));
    }
}
