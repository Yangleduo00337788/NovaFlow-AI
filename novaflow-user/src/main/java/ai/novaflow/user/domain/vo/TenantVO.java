package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TenantVO {

    private Long id;
    private String tenantCode;
    private String tenantName;
    private String logoUrl;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String planType;
    private String planTypeLabel;
    private Integer status;
    private LocalDateTime expireAt;
    private Integer maxMembers;
    private Integer memberCount;
    private Integer maxAgents;
    private Integer maxKnowledge;
    private Integer maxStorageMb;
    private Long monthlyTokenQuota;
    private LocalDateTime createdAt;
}
