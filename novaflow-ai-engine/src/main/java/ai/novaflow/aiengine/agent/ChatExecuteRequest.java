package ai.novaflow.aiengine.agent;

import ai.novaflow.model.domain.ResolvedModelConfig;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatExecuteRequest {

    private ResolvedModelConfig modelConfig;
    private String systemPrompt;
    private String userMessage;
    private String conversationId;
    private Integer memoryWindow;
    private java.util.List<ai.novaflow.tool.domain.HttpToolDefinition> tools;
}
