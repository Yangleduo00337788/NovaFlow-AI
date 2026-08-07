package ai.novaflow.model.domain.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ModelUsageRecordRequest {

    private Long tenantId;
    private Long applicationId;
    private Long agentId;
    private Long userId;
    private Long modelConfigId;
    private String usageType;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private Long latencyMs;
}
