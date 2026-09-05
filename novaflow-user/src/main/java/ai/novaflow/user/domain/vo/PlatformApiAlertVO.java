package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlatformApiAlertVO {

    private String type;
    private String severity;
    private Long tenantId;
    private String tenantName;
    private String message;
    private Long metricValue;
    private Long threshold;
}
