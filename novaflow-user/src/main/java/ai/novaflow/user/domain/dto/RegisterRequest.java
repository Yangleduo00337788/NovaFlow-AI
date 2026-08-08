package ai.novaflow.user.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度需在 8-64 位之间")
    private String password;

    @NotBlank(message = "请确认密码")
    private String confirmPassword;

    @Size(max = 64, message = "昵称不能超过 64 个字符")
    private String nickname;

    @NotBlank(message = "企业名称不能为空")
    @Size(max = 128, message = "企业名称不能超过 128 个字符")
    private String companyName;
}
