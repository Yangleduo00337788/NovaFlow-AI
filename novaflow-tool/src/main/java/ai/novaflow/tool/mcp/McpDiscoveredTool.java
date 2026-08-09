package ai.novaflow.tool.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpDiscoveredTool {

    private String name;
    private String description;
    private Map<String, Object> inputSchema;
}
