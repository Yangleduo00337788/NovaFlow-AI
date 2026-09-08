package ai.novaflow.agent.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class AgentEmbedConfigRequest {

    private String themeColor;
    private List<String> allowedDomains;
    private String postMessageTargetOrigin;
}
