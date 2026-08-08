package ai.novaflow.workflow.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WorkflowRunResultVO {

    private String executionId;
    private Integer status;
    private String output;
    private String errorMessage;
    private Integer durationMs;
    private List<WorkflowRunStepVO> steps;
}
