package ai.novaflow.agent.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AgentEmbedConfigVO {

    private String themeColor;
    private List<String> allowedDomains;
    private String postMessageTargetOrigin;
}
