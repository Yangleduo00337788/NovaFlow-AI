package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PlatformTenantDetailVO {

    private PlatformTenantVO tenant;
    private int agentCount;
    private int knowledgeCount;
    private int applicationCount;
    private int workflowCount;
    private Integer memberUsedPercent;
    private Integer tokenUsedPercent;
    private Integer storageUsedPercent;
    private long callsThisMonth;
    private BigDecimal costCnyThisMonth;
    private boolean expired;
    private Integer daysUntilExpiry;
    private List<PlatformTrendPointVO> dailyTokenTrend;
    private List<PlatformModelUsageVO> topModelsThisMonth;
}
