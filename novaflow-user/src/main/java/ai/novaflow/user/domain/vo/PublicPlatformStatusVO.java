package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublicPlatformStatusVO {

    private boolean maintenanceEnabled;
    private String maintenanceMessage;
    private String platformAnnouncement;
}
