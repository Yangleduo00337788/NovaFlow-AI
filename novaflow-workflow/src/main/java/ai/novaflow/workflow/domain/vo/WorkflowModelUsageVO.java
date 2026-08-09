package ai.novaflow.workflow.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowModelUsageVO {

    private String nodeId;
    private String nodeName;
    private Long modelConfigId;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private Integer latencyMs;
}
