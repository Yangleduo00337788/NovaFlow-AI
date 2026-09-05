package ai.novaflow.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProdLoggingConfigTest {

    @Test
    void prodProfileDoesNotUseDebugLoggingForNovaFlowPackage() throws IOException {
        String yaml = new ClassPathResource("application-prod.yml")
                .getContentAsString(StandardCharsets.UTF_8);
        assertTrue(yaml.contains("ai.novaflow: info"), "prod profile should set ai.novaflow to info");
        assertFalse(yaml.contains("ai.novaflow: debug"), "prod profile must not enable debug logging");
    }
}
