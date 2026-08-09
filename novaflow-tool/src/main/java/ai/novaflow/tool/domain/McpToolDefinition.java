package ai.novaflow.tool.domain;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class McpToolDefinition {

    private Long mcpServerId;
    private String mcpToolName;
    private String sourceServerName;
    private Map<String, Object> inputSchema;
}
