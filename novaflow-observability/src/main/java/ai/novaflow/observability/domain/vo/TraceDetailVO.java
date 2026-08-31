package ai.novaflow.observability.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TraceDetailVO {

    private String traceId;
    private String spanType;
    private String spanTypeLabel;
    private String name;
    private Integer status;
    private String statusLabel;
    private Integer durationMs;
    private String durationLabel;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String errorMessage;
    private List<TraceNodeVO> nodes;
}
