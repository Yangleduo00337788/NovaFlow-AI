package ai.novaflow.user.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Table("platform_security_alert_event")
public class PlatformSecurityAlertEventEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private String alertType;
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
    private LocalDate eventDate;
    private String dedupeKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
