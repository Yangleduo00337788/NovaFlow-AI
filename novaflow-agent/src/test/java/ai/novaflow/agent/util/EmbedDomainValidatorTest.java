package ai.novaflow.agent.util;

import ai.novaflow.agent.domain.AgentEmbedConfig;
import ai.novaflow.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmbedDomainValidatorTest {

    @Test
    void emptyWhitelistAllowsAnyOrigin() {
        AgentEmbedConfig config = AgentEmbedConfig.builder().allowedDomains(List.of()).build();
        assertDoesNotThrow(() -> EmbedDomainValidator.requireAllowed(config, "https://evil.example/app", "https://evil.example"));
    }

    @Test
    void matchingHostIsAllowed() {
        AgentEmbedConfig config = AgentEmbedConfig.builder()
                .allowedDomains(List.of("partner.example", "*.acme.test"))
                .build();
        assertDoesNotThrow(() -> EmbedDomainValidator.requireAllowed(
                config, "https://partner.example/chat", null));
        assertDoesNotThrow(() -> EmbedDomainValidator.requireAllowed(
                config, null, "https://app.acme.test"));
    }

    @Test
    void foreignHostIsRejected() {
        AgentEmbedConfig config = AgentEmbedConfig.builder()
                .allowedDomains(List.of("partner.example"))
                .build();
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> EmbedDomainValidator.requireAllowed(config, "https://evil.example/page", "https://evil.example"));
        assertEquals(40304, ex.getCode());
    }
}
