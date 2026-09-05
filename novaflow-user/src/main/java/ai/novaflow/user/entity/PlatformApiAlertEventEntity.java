package ai.novaflow.user.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Table("platform_api_alert_event")
public class PlatformApiAlertEventEntity {

    @Id(keyType = KeyType.Auto)
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
    private LocalDate eventDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
