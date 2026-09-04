package ai.novaflow.tool.mcp;

import ai.novaflow.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpCommandValidatorTest {

    private final McpCommandValidator validator = new McpCommandValidator("npx,node,python", true);
    private final McpCommandValidator stdioDisabledValidator = new McpCommandValidator("npx,node,python", false);

    @Test
    void allowsWhitelistedCommand() {
        McpServerConfig config = McpServerConfig.builder()
                .transportType("stdio")
                .command("npx")
                .args(List.of("-y", "@modelcontextprotocol/server-filesystem", "/tmp"))
                .build();
        assertDoesNotThrow(() -> validator.validate(config));
    }

    @Test
    void rejectsUnknownCommand() {
        McpServerConfig config = McpServerConfig.builder()
                .transportType("stdio")
                .command("bash")
                .args(List.of("-c", "rm -rf /"))
                .build();
        assertThrows(BusinessException.class, () -> validator.validate(config));
    }

    @Test
    void rejectsDangerousEnv() {
        McpServerConfig config = McpServerConfig.builder()
                .transportType("stdio")
                .command("node")
                .env(Map.of("LD_PRELOAD", "/tmp/evil.so"))
                .build();
        assertThrows(BusinessException.class, () -> validator.validate(config));
    }

    @Test
    void rejectsStdioWhenDisabled() {
        McpServerConfig config = McpServerConfig.builder()
                .transportType("stdio")
                .command("npx")
                .build();
        assertThrows(BusinessException.class, () -> stdioDisabledValidator.validate(config));
    }
}
