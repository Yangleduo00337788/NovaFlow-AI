package ai.novaflow.agent.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private String systemPrompt;
    private String welcomeMessage;
    private BigDecimal temperature;
    private Integer maxTokens;
    private String memoryType;
    private Integer memoryWindow;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
