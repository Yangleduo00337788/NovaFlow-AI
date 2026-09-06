package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PlatformSecurityAlertEventVO {

    private Long id;
    private String alertType;
    private String alertTypeLabel;
    private String severity;
    private Long userId;
    private String userEmail;
    private Long tenantId;
    private String clientIp;
    private String userAgent;
    private String message;
    private Long metricValue;
    private Long threshold;
    private String status;
    private Long ackedBy;
    private LocalDateTime ackedAt;
    private LocalDateTime createdAt;
}
