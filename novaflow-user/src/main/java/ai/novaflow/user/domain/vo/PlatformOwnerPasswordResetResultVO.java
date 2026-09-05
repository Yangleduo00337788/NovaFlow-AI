package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlatformOwnerPasswordResetResultVO {

    private Long ownerId;
    private String ownerEmail;
    private String generatedPassword;
    private boolean inviteEmailSent;
}
