package ai.novaflow.workflow.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class WorkflowVO {

    private Long id;
    private Long applicationId;
    private String applicationName;
    private String workflowName;
    private String description;
    private Integer status;
    private Integer version;
    private Integer nodeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
