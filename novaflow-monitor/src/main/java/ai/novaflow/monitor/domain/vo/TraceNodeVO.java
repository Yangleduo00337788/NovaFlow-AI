package ai.novaflow.monitor.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TraceNodeVO {

    private String nodeId;
    private String nodeName;
    private String nodeType;
    private Integer status;
    private String statusLabel;
    private Integer durationMs;
    private String durationLabel;
    private String errorMessage;
    private LocalDateTime startedAt;
}
