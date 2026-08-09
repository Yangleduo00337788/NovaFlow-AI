package ai.novaflow.tool.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ToolDefinitionVO {

    private Long id;
    private String toolName;
    private String displayName;
    private String description;
    private String toolType;
    private String method;
    private String url;
    private Long mcpServerId;
    private String mcpToolName;
    private String sourceServerName;
    private String bodyTemplate;
    private Map<String, String> headers;
    private Map<String, Object> inputSchema;
    private String skillFileName;
    private String skillContentPreview;
    private String skillContent;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
