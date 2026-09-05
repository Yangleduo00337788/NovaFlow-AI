package ai.novaflow.model.domain;

import lombok.Data;

@Data
public class TenantUsageAggregate {

    private Long tenantId;
    private String tenantName;
    private Long calls;
    private Long tokens;
}
