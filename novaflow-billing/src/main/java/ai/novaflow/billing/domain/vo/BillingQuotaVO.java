package ai.novaflow.billing.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BillingQuotaVO {

    private String planType;
    private String planTypeLabel;
    private LocalDateTime expireAt;
    private Long monthlyTokenQuota;
    private Long usedTokens;
    private Integer tokenUsedPercent;
    private Integer memberCount;
    private Integer maxMembers;
    private Integer memberUsedPercent;
    private Integer maxAgents;
    private Integer maxKnowledge;
}
