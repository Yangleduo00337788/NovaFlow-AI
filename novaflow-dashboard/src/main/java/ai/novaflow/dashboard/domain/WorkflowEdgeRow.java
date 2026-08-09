package ai.novaflow.dashboard.domain;

import lombok.Data;

@Data
public class WorkflowEdgeRow {

    private String edgeId;
    private String sourceNodeId;
    private String targetNodeId;
    private String sourceHandle;
    private String targetHandle;
    private String conditionExpr;
}
