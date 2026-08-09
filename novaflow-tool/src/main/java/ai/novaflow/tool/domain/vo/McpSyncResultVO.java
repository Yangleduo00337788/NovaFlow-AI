package ai.novaflow.tool.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class McpSyncResultVO {

    private Long mcpServerId;
    private String serverName;
    private Integer created;
    private Integer updated;
    private Integer removed;
    private Integer total;
    private Integer syncedToolCount;
    private String message;
    private List<String> toolNames;
}
