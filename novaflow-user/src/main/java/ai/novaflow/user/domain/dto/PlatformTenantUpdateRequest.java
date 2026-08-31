package ai.novaflow.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlatformTenantUpdateRequest {

    @NotBlank(message = "企业名称不能为空")
    private String tenantName;

    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String planType;
    private Integer status;
    private LocalDateTime expireAt;
    private Integer maxMembers;
    private Integer maxAgents;
    private Integer maxKnowledge;
    private Integer maxStorageMb;
    private Long monthlyTokenQuota;
}
