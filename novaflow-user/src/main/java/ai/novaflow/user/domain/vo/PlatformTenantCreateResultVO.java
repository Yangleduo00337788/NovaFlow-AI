package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlatformTenantCreateResultVO {

    private PlatformTenantVO tenant;
    private Long ownerId;
    private String ownerEmail;
    /** 仅当平台代生成密码时返回，供运营人员一次性抄送 */
    private String generatedPassword;
    private boolean inviteEmailSent;
}
