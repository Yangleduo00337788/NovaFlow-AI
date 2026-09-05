package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PlatformDashboardOverviewVO {

    private PlatformGlobalStatsVO stats;
    private List<PlatformTrendPointVO> tenantGrowthTrend;
    private List<PlatformTrendPointVO> tokenUsageTrend;
    private List<PlatformTenantHealthVO> tenantHealth;
}
