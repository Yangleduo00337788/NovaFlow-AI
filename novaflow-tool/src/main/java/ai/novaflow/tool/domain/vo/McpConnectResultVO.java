package ai.novaflow.tool.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class McpConnectResultVO {

    private Long id;
    private String serverName;
    private Integer status;
    private String statusLabel;
    private Integer toolCount;
    private String message;
    private LocalDateTime lastConnectedAt;
    private List<McpDiscoveredToolVO> tools;
}
