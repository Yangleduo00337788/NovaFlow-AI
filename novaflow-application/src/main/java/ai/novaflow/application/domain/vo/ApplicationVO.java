package ai.novaflow.application.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ApplicationVO {

    private Long id;
    private Long workspaceId;
    private String appName;
    private String description;
    private String icon;
    private String appType;
    private Long defaultAgentId;
    private String defaultAgentName;
    private Integer publishStatus;
    private String accessType;
    private Long invokeCount;
    private LocalDateTime publishedAt;
    private Integer status;
    private Integer agentCount;
    private Integer knowledgeBaseCount;
    private List<Long> agentIds;
    private List<Long> knowledgeBaseIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
