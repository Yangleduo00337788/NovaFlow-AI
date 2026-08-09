package ai.novaflow.tool.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class McpDiscoveredToolVO {

    private String name;
    private String description;
    private Map<String, Object> inputSchema;
}
