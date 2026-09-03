package ai.novaflow.billing.domain.dto;

import lombok.Data;

@Data
public class NotifyChannelSaveRequest {

    private Boolean emailEnabled;
    private String emailRecipients;
    private Boolean webhookEnabled;
    private String webhookUrl;
    private String webhookSecret;
}
