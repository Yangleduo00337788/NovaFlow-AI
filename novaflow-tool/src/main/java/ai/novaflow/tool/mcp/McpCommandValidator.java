package ai.novaflow.tool.mcp;

import ai.novaflow.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MCP stdio 命令白名单校验，防止租户通过任意命令在宿主机执行代码。
 */
@Component
public class McpCommandValidator {

    private final Set<String> allowedCommands;
    private final boolean stdioEnabled;

    public McpCommandValidator(
            @Value("${novaflow.mcp.allowed-commands:npx,node,uvx,uv,python,python3}") String allowedCommands,
            @Value("${novaflow.mcp.stdio-enabled:true}") boolean stdioEnabled) {
        this.stdioEnabled = stdioEnabled;
        this.allowedCommands = Arrays.stream(allowedCommands.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    public void validate(McpServerConfig config) {
        if (config == null) {
            return;
        }
        if (!"stdio".equalsIgnoreCase(config.getTransportType())) {
            return;
        }
        if (!stdioEnabled) {
            throw new BusinessException("当前环境已禁用 MCP stdio 传输，请使用 SSE/HTTP 方式");
        }
        String command = config.getCommand();
        if (!StringUtils.hasText(command)) {
            throw new BusinessException("stdio 配置缺少 command 字段");
        }
        String normalized = normalizeCommand(command.trim());
        if (!allowedCommands.contains(normalized)) {
            throw new BusinessException("MCP 命令不在白名单内: " + normalized
                    + "。允许: " + String.join(", ", allowedCommands));
        }
        validateArgs(config.getArgs());
        validateEnv(config.getEnv());
    }

    private String normalizeCommand(String command) {
        String name = command;
        int slash = Math.max(command.lastIndexOf('/'), command.lastIndexOf('\\'));
        if (slash >= 0 && slash < command.length() - 1) {
            name = command.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        return name.toLowerCase(Locale.ROOT);
    }

    private void validateArgs(List<String> args) {
        if (args == null) {
            return;
        }
        for (String arg : args) {
            if (!StringUtils.hasText(arg)) {
                continue;
            }
            String trimmed = arg.trim();
            if (trimmed.contains("..") || trimmed.contains("|") || trimmed.contains("&") || trimmed.contains(";")) {
                throw new BusinessException("MCP 参数包含非法字符");
            }
        }
    }

    private void validateEnv(java.util.Map<String, String> env) {
        if (env == null || env.isEmpty()) {
            return;
        }
        for (String key : env.keySet()) {
            if (key == null) {
                continue;
            }
            String upper = key.toUpperCase(Locale.ROOT);
            if (upper.contains("LD_PRELOAD") || upper.contains("DYLD_INSERT_LIBRARIES")) {
                throw new BusinessException("MCP 环境变量不允许注入动态库: " + key);
            }
        }
    }
}
