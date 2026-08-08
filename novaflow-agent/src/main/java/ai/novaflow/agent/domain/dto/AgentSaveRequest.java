package ai.novaflow.agent.domain.dto;

import ai.novaflow.tool.domain.HttpToolDefinition;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AgentSaveRequest {

    @NotBlank(message = "Agent名称不能为空")
    private String agentName;
    private String description;
    private String icon;
    private String agentType = "chat";
    private Long applicationId;
    private String systemPrompt;
    private String welcomeMessage;
    private Long modelConfigId;
    private BigDecimal temperature = new BigDecimal("0.70");
    private Integer maxTokens = 2048;
    private String memoryType = "window";
    private Integer memoryWindow = 10;
    private Integer retrievalTopK = 5;
    private Float retrievalScoreThreshold;
    private Boolean rerankEnabled;
    private String rerankModel;
    private Integer rerankCandidateK;
    private List<Long> knowledgeBaseIds;
    private List<HttpToolDefinition> tools;
}
