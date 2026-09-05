package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PlatformApiAlertEventVO {

    private Long id;
    private String alertType;
    private String severity;
    private Long tenantId;
    private String tenantName;
    private String message;
    private Long metricValue;
    private Long threshold;
    private String status;
    private Long ackedBy;
    private LocalDateTime ackedAt;
    private LocalDateTime createdAt;
}
