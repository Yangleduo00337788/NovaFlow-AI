package ai.novaflow.workflowengine.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowStepSnapshot {

    private String nodeId;
    private String nodeType;
    private String nodeName;
    private int status;
    private String input;
    private String output;
    private String errorMessage;
    private int durationMs;
    private int tokensUsed;
    private Object modelUsage;
}
