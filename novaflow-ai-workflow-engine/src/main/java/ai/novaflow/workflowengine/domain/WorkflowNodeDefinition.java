package ai.novaflow.workflowengine.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowNodeDefinition {

    private String nodeId;
    private String nodeType;
    private String nodeName;
    private String nodeConfig;
    private Long workflowId;
}
