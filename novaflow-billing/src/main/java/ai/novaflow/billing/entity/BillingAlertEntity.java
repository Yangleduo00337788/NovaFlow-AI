package ai.novaflow.billing.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("billing_alert")
public class BillingAlertEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private String alertName;
    private String alertType;
    private Integer thresholdPercent;
    private String notifyChannels;
    private Integer isEnabled;
    private LocalDateTime lastTriggeredAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
