package ai.novaflow.workflow.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("workflow_node_log")
public class WorkflowNodeLogEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private String executionId;
    private String nodeId;
    private String nodeType;
    private Integer status;
    private String inputData;
    private String outputData;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer durationMs;
}
