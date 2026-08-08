package ai.novaflow.workflow.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Table("workflow_node")
public class WorkflowNodeEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private Long workflowId;
    private String nodeId;
    private String nodeType;
    private String nodeName;
    private BigDecimal positionX;
    private BigDecimal positionY;
    private String nodeConfig;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
