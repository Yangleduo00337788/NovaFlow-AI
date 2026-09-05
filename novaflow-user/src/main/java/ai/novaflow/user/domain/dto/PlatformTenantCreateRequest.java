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

    /** 手动指定初始密码；与 generatePassword 二选一 */
    private String ownerPassword;

    /** 为 true 时自动生成符合规则的初始密码 */
    private Boolean generatePassword;

    /** 创建成功后向 Owner 发送邀请邮件（需配置 SMTP） */
    private Boolean sendInviteEmail;

    private String ownerNickname;

    private String contactName;
    private String contactEmail;
    private String contactPhone;
}
