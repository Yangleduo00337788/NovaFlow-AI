package ai.novaflow.observability.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TraceSpanRow {

    private String traceId;
    private String spanType;
    private String name;
    private Integer status;
    private Integer durationMs;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String errorMessage;
}
