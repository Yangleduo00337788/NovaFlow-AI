package ai.novaflow.tool.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class McpServerVO {

    private Long id;
    private String serverName;
    private String description;
    private String transportType;
    private String endpoint;
    private Integer status;
    private String statusLabel;
    private Integer toolCount;
    private LocalDateTime lastConnectedAt;
    private LocalDateTime updatedAt;
}
