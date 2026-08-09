package ai.novaflow.workflow.domain.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowRunOptions {

    private Long triggeredByUserId;
    private Long agentId;
    @Builder.Default
    private boolean recordUsage = true;
}
