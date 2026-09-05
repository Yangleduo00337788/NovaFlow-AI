package ai.novaflow.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RoleSaveRequest {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称过长")
    private String roleName;

    @Size(max = 256, message = "描述过长")
    private String description;

    @NotEmpty(message = "至少选择一个权限")
    private List<String> permissionCodes;
}
