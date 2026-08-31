package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlatformGlobalStatsVO {

    private long tenantCount;
    private long activeTenantCount;
    private long totalMembers;
    private long totalAgents;
    private long totalKnowledgeBases;
    private long totalWorkflows;
    private long tokensUsedThisMonth;
}
