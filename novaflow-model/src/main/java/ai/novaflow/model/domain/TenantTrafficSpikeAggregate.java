package ai.novaflow.model.domain;

import lombok.Data;

@Data
public class TenantTrafficSpikeAggregate {

    private Long tenantId;
    private String tenantName;
    private Long todayCalls;
    private Long avgDailyCalls;
}
