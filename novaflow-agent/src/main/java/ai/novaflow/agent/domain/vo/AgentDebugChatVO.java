package ai.novaflow.agent.domain.vo;

import ai.novaflow.chat.domain.vo.RetrievalSourceVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AgentDebugChatVO {

    private String reply;
    private String agentName;
    private Integer tokensUsed;
    private Long latencyMs;
    private Boolean debugMode;
    private List<RetrievalSourceVO> sources;
    private ModelCapabilitiesVO modelCapabilities;
    private String modelName;
    private String providerName;
}
