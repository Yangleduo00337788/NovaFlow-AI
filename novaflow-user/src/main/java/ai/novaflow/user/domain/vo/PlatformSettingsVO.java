package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PlatformSettingsVO {

    private boolean registrationEnabled;
    private long hourlyCallsThreshold;
    private double trafficSpikeMultiplier;
    private List<String> allowedProviderCodes;
    private boolean providerWhitelistEnabled;
    private boolean maintenanceEnabled;
    private String maintenanceMessage;
    private String platformAnnouncement;
    private boolean abnormalLoginEnabled;
    private boolean newUserAgentEnabled;
    private int batchRegisterIpLimitPerDay;
    private int storageWarnPercent;
}
