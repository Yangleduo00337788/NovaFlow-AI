package ai.novaflow.application.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PortalAppVO {

    private Long id;
    private String appName;
    private String description;
    private String icon;
    private String appType;
    private Long defaultAgentId;
    private String defaultAgentName;
    private LocalDateTime publishedAt;
    private String portalPath;
}
