package ai.novaflow.user.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MemberInviteRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    private String nickname;

    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "tenant_admin|developer|user", message = "角色编码无效")
    private String roleCode;

    private String password;
}
