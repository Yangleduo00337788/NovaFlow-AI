package ai.novaflow.billing.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BillingOverviewVO {

    private String periodLabel;
    private Long totalCalls;
    private Long totalTokens;
    private String totalCostLabel;
    private String tokenChangePercent;
    private String callChangePercent;
    private List<BillingMetricVO> metrics;
    private List<BillingTrendPointVO> dailyTrend;
    private List<BillingUsageTypeVO> usageByType;
    private List<BillingModelUsageVO> topModels;
    private BillingQuotaVO quota;
}
