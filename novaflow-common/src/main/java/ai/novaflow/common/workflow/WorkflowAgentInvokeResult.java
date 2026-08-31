package ai.novaflow.common.workflow;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowAgentInvokeResult {

    private boolean success;
    private String output;
    private String errorMessage;
    private Integer tokensUsed;

    public static WorkflowAgentInvokeResult success(String output, Integer tokensUsed) {
        return WorkflowAgentInvokeResult.builder()
                .success(true)
                .output(output)
                .tokensUsed(tokensUsed != null ? tokensUsed : 0)
                .build();
    }

    public static WorkflowAgentInvokeResult failure(String errorMessage) {
        return WorkflowAgentInvokeResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .tokensUsed(0)
                .build();
    }
}
