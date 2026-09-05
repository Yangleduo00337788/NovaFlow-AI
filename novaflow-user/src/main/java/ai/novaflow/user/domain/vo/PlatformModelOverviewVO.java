package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PlatformModelOverviewVO {

    private long totalProviders;
    private long enabledProviders;
    private long totalModelConfigs;
    private long enabledModelConfigs;
    private List<PlatformProviderStatVO> providersByCode;
}
