package ai.novaflow.workflow.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowEdgeVO {

    private String edgeId;
    private String sourceNodeId;
    private String targetNodeId;
    private String sourceHandle;
    private String targetHandle;
    private String condition;
}
