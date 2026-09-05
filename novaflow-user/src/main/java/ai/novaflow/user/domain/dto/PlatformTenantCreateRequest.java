package ai.novaflow.user.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlatformTenantCreateRequest {

    @NotBlank(message = "企业名称不能为空")
    private String tenantName;

    private String planType;

    @NotBlank(message = "所有者邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String ownerEmail;

    @NotBlank(message = "初始密码不能为空")
    private String ownerPassword;

    private String ownerNickname;

    private String contactName;
    private String contactEmail;
    private String contactPhone;
}
