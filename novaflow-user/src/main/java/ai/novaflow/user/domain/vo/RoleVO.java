package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoleVO {

    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private Boolean isSystem;
    private Integer memberCount;
    private List<String> permissionCodes;
}
