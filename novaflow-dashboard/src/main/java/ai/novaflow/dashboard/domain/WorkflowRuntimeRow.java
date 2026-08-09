package ai.novaflow.dashboard.domain;

import lombok.Data;

@Data
public class WorkflowRuntimeRow {

    private String executionId;
    private Long workflowId;
    private String workflowName;
    private Integer status;
}
