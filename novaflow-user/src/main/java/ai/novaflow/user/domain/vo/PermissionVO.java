package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PermissionVO {

    private Long id;
    private String permissionCode;
    private String permissionName;
    private String module;
}
