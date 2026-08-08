package ai.novaflow.workflow.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class WorkflowNodeVO {

    private String nodeId;
    private String nodeType;
    private String nodeName;
    private Double positionX;
    private Double positionY;
    private Map<String, Object> nodeConfig;
}
