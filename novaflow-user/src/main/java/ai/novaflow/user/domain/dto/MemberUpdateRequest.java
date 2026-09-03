package ai.novaflow.user.domain.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MemberUpdateRequest {

    @Pattern(regexp = "tenant_admin|developer|user", message = "角色编码无效")
    private String roleCode;

    private Integer status;

    private Long departmentId;
}
