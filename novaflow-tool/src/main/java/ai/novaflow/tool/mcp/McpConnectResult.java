package ai.novaflow.tool.mcp;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class McpConnectResult {

    private boolean success;
    private String message;
    private String protocolVersion;
    private List<McpDiscoveredTool> tools;
}
