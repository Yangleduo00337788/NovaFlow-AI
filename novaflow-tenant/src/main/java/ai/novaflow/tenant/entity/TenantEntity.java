package ai.novaflow.tenant.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("tenant")
public class TenantEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private String tenantCode;
    private String tenantName;
    private String logoUrl;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
