package ai.novaflow.observability.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TraceSpanVO {

    private String traceId;
    private String spanType;
    private String spanTypeLabel;
    private String name;
    private Integer status;
    private String statusLabel;
    private Integer durationMs;
    private String durationLabel;
    private LocalDateTime startedAt;
    private String errorMessage;
}
