package ai.novaflow.application.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PortalAppDetailVO {

    private Long applicationId;
    private String appName;
    private String description;
    private Long defaultAgentId;
    private String defaultAgentName;
    private String portalPath;
}
