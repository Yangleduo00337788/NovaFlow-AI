package ai.novaflow.dashboard.domain;

import lombok.Data;

@Data
public class WorkflowNodeRow {

    private String nodeId;
    private String nodeName;
    private String nodeType;
    private Integer sortOrder;
    private java.math.BigDecimal positionX;
    private java.math.BigDecimal positionY;
}
