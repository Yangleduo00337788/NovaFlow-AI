package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlatformTenantTrafficSpikeVO {

    private Long tenantId;
    private String tenantName;
    private Long todayCalls;
    private Long avgDailyCalls;
    private Double spikeRatio;
}
