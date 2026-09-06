package ai.novaflow.user.domain.dto;

import lombok.Data;

@Data
public class PlatformNotifyChannelSaveRequest {

    private Boolean emailEnabled;
    private String emailRecipients;
    private Boolean webhookEnabled;
    private String webhookUrl;
    private String webhookSecret;
}
