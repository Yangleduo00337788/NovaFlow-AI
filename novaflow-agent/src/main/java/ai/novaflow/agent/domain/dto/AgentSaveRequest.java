package ai.novaflow.agent.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

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
    private BigDecimal temperature = new BigDecimal("0.70");
    private Integer maxTokens = 2048;
    private String memoryType = "window";
    private Integer memoryWindow = 10;
}
