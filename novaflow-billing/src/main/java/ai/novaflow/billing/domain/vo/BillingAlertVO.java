package ai.novaflow.billing.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BillingAlertVO {

    private Long id;
    private String alertName;
    private String alertType;
    private Integer thresholdPercent;
    private Boolean enabled;
    private List<String> notifyChannels;
    private LocalDateTime lastTriggeredAt;
}
