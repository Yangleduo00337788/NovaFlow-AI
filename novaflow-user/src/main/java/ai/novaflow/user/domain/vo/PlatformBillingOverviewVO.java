package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PlatformBillingOverviewVO {

    private String month;
    private long totalTokens;
    private long prevMonthTokens;
    private long totalCalls;
    private BigDecimal costCny;
    private BigDecimal costUsd;
    private List<PlatformTrendPointVO> dailyTrend;
    private List<PlatformTenantUsageVO> topTenants;
    private List<PlatformModelUsageVO> topModels;
}
