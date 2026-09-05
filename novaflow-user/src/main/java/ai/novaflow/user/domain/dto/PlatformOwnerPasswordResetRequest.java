package ai.novaflow.user.domain.dto;

import lombok.Data;

@Data
public class PlatformOwnerPasswordResetRequest {

    /** 手动指定新密码；与 generatePassword 二选一 */
    private String newPassword;

    /** 为 true 时自动生成新密码并在响应中返回 */
    private Boolean generatePassword;

    /** 重置后发送通知邮件 */
    private Boolean sendInviteEmail;
}
