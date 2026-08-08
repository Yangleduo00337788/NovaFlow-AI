package ai.novaflow.agent.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AgentDebugStreamEvent {

    private String type;
    private String content;
    private String reply;
    private String thinking;
    private String agentName;
    private Integer tokensUsed;
    private Long latencyMs;
    private Boolean debugMode;
    private String message;
    private List<RetrievalSourceVO> sources;
}
