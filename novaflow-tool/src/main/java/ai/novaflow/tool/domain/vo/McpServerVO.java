package ai.novaflow.tool.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class McpServerVO {

    private Long id;
    private String serverName;
    private String description;
    private String transportType;
    private String commandSummary;
    private String serverConfig;
    private Integer status;
    private String statusLabel;
    private Integer toolCount;
    private Integer syncedToolCount;
    private LocalDateTime lastConnectedAt;
    private LocalDateTime updatedAt;
    private List<McpDiscoveredToolVO> tools;
}
