package ai.novaflow.dashboard.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PublishedWorkflowRow {

    private Long workflowId;
    private String workflowName;
    private Integer status;
    private String applicationName;
    private LocalDateTime updatedAt;
}
