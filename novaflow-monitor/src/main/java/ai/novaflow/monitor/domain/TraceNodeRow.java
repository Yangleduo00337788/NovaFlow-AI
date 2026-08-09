package ai.novaflow.monitor.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TraceNodeRow {

    private String nodeId;
    private String nodeName;
    private String nodeType;
    private Integer status;
    private Integer durationMs;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
