package ai.novaflow.agent.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Table("agent_config")
public class AgentConfigEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private Long agentId;
    private String systemPrompt;
    private Long promptTemplateId;
    private Long promptTemplateVersionId;
    private String promptRefMode;
    private String welcomeMessage;
    private Long modelConfigId;
    private BigDecimal temperature;
    private Integer maxTokens;
    private String memoryType;
    private Integer memoryWindow;
    private Long workflowId;
    private String retrievalConfig;
    private String extraConfig;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
