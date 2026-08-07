package ai.novaflow.agent.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentDebugChatVO {

    private String reply;
    private String agentName;
    private Integer tokensUsed;
    private Long latencyMs;
    private Boolean debugMode;
}
