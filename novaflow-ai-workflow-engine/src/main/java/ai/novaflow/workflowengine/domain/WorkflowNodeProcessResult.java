package ai.novaflow.workflowengine.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowNodeProcessResult {

    private boolean success;
    private String output;
    private String errorMessage;
    private int tokensUsed;
    private Object modelUsage;
}
