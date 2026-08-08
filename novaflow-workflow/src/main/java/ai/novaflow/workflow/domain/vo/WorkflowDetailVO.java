package ai.novaflow.workflow.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class WorkflowDetailVO {

    private Long id;
    private Long applicationId;
    private String applicationName;
    private String workflowName;
    private String description;
    private Integer status;
    private Integer version;
    private String elExpression;
    private Map<String, Object> canvasData;
    private List<WorkflowNodeVO> nodes;
    private List<WorkflowEdgeVO> edges;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
