package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PlatformApiMonitorVO {

    private long totalCallsToday;
    private long totalCallsLastHour;
    private long hourlyCallsThreshold;
    private double trafficSpikeMultiplier;
    private List<PlatformApiAlertVO> alerts;
    private List<PlatformTenantUsageVO> topTenantsLastHour;
    private List<PlatformTenantTrafficSpikeVO> trafficSpikes;
}
