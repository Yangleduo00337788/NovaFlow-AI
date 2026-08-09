package ai.novaflow.tool.executor;

import ai.novaflow.tool.domain.HttpToolDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ToolExecutorRouter {

    private final HttpToolExecutor httpToolExecutor;
    private final McpToolExecutor mcpToolExecutor;

    public String execute(HttpToolDefinition tool, Map<String, Object> arguments) {
        if (tool != null && "mcp".equalsIgnoreCase(tool.getToolType())) {
            return mcpToolExecutor.execute(tool, arguments);
        }
        return httpToolExecutor.execute(tool, arguments);
    }
}
