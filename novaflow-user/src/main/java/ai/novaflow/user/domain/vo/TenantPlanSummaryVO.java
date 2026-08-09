package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TenantPlanSummaryVO {

    private String planType;
    private String planTypeLabel;
    private LocalDateTime expireAt;
    private Integer memberCount;
    private Integer maxMembers;
    private Integer usedPercent;
}
