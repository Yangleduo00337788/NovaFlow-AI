package ai.novaflow.aiengine.agent;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatExecuteResult {

    private String reply;
    private String thinking;
    private Integer tokensUsed;
    private Integer inputTokens;
    private Integer outputTokens;
    private Long latencyMs;
    private String modelName;
}
