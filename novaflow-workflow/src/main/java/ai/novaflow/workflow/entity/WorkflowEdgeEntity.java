package ai.novaflow.workflow.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("workflow_edge")
public class WorkflowEdgeEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private Long workflowId;
    private String edgeId;
    private String sourceNodeId;
    private String targetNodeId;
    private String sourceHandle;
    private String targetHandle;
    private String condition;
    private LocalDateTime createdAt;
}
