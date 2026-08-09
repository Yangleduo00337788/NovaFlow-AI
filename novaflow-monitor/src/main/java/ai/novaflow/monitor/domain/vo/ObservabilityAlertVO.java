package ai.novaflow.monitor.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ObservabilityAlertVO {

    private String key;
    private String level;
    private String title;
    private String message;
    private boolean active;
}
