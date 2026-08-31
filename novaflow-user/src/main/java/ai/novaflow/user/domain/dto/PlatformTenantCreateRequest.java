package ai.novaflow.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlatformTenantCreateRequest {

    @NotBlank(message = "企业名称不能为空")
    private String tenantName;

    private String contactName;
    private String contactEmail;
    private String contactPhone;

    @NotBlank(message = "套餐类型不能为空")
    private String planType;

    private LocalDateTime expireAt;
    private Integer maxMembers;
    private Integer maxAgents;
    private Integer maxKnowledge;
    private Integer maxStorageMb;
    private Long monthlyTokenQuota;

    @NotNull(message = "管理员用户 ID 不能为空")
    private Long adminUserId;
}
