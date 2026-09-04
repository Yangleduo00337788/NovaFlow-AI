package ai.novaflow.workflow.domain;

import lombok.Data;

@Data
public class WorkflowNodeCountRow {

    private Long workflowId;
    private Long nodeCount;
}
