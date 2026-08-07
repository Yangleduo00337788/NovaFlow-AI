package ai.novaflow.agent.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentDebugStreamEvent {

    private String type;
    private String content;
    private String reply;
    private String agentName;
    private Integer tokensUsed;
    private Long latencyMs;
    private Boolean debugMode;
    private String message;
}
