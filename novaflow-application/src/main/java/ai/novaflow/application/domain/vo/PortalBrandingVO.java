package ai.novaflow.application.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PortalBrandingVO {

    private String tenantName;
    private String logoUrl;
    private String portalThemeColor;
    private String portalSubtitle;
}
