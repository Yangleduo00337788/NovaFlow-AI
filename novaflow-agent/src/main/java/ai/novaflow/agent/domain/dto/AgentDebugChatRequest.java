package ai.novaflow.agent.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgentDebugChatRequest {

    @NotBlank(message = "消息不能为空")
    private String message;
    private String conversationId;
    private Boolean enableDeepThinking;
    private Boolean enableWebSearch;
    private String attachmentName;
    private String attachmentContext;
}
