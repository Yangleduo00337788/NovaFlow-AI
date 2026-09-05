package ai.novaflow.user.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class PlatformSettingsUpdateRequest {

    private Boolean registrationEnabled;
    private Long hourlyCallsThreshold;
    private Double trafficSpikeMultiplier;
    private List<String> allowedProviderCodes;
    private Boolean maintenanceEnabled;
    private String maintenanceMessage;
    private String platformAnnouncement;
}
