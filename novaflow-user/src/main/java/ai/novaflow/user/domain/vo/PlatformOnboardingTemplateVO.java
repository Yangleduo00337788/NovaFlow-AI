package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlatformOnboardingTemplateVO {

    private String planType;
    private String planTypeLabel;
    private Integer maxMembers;
    private Integer maxAgents;
    private Integer maxKnowledge;
    private Integer maxStorageMb;
    private Long monthlyTokenQuota;
}
