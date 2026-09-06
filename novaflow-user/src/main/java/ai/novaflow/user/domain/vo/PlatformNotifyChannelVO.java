package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlatformNotifyChannelVO {

    private boolean emailEnabled;
    private String emailRecipients;
    private boolean webhookEnabled;
    private String webhookUrl;
    private boolean webhookSecretSet;
    private boolean mailConfigured;
}
