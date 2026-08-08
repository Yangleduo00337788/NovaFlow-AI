package ai.novaflow.application.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ApplicationSaveRequest {

    @NotBlank(message = "应用名称不能为空")
    private String appName;
    private String description;
    private String icon;
    private String appType = "agent";
    private String accessType = "team";
    private Long defaultAgentId;
    private List<Long> agentIds;
    private List<Long> knowledgeBaseIds;
}
