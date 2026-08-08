package ai.novaflow.workflow.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("workflow_execution")
public class WorkflowExecutionEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private Long workflowId;
    private String executionId;
    private Integer status;
    private String inputData;
    private String outputData;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer durationMs;
    private Long triggeredBy;
}
