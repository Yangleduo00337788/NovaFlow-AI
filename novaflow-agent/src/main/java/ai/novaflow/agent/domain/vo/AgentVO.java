package ai.novaflow.agent.domain.vo;

import ai.novaflow.tool.domain.HttpToolDefinition;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AgentVO {

    private Long id;
    private Long applicationId;
    private String agentName;
    private String description;
    private String icon;
    private String agentType;
    private Integer status;
    private Integer version;
    private LocalDateTime publishedAt;
    private String systemPrompt;
    private Long promptTemplateId;
    private Long promptTemplateVersionId;
    private String promptRefMode;
    private Integer promptTemplateCurrentVersion;
    private String welcomeMessage;
    private Long modelConfigId;
    private BigDecimal temperature;
    private Integer maxTokens;
    private String memoryType;
    private Integer memoryWindow;
    private Long workflowId;
    private String workflowName;
    private Integer retrievalTopK;
    private Float retrievalScoreThreshold;
    private Boolean rerankEnabled;
    private String rerankModel;
    private Integer rerankCandidateK;
    private Boolean hybridEnabled;
    private Float hybridAlpha;
    private List<Long> knowledgeBaseIds;
    private List<Long> toolIds;
    private List<Long> skillIds;
    private List<HttpToolDefinition> tools;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
