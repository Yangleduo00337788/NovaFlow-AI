package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlatformUserMembershipVO {

    private Long tenantId;
    private String tenantName;
    private String roleCode;
    private String roleName;
    private Integer status;
}
