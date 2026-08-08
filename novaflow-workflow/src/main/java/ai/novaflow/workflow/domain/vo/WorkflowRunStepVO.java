package ai.novaflow.workflow.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowRunStepVO {

    private String nodeId;
    private String nodeType;
    private String nodeName;
    private Integer status;
    private String input;
    private String output;
    private String errorMessage;
    private Integer durationMs;
}
