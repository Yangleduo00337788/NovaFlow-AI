package ai.novaflow.application.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("application")
public class ApplicationEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private Long workspaceId;
    private String appName;
    private String description;
    private String icon;
    private String appType;
    private Long defaultAgentId;
    private Integer publishStatus;
    private String accessType;
    private Long invokeCount;
    private LocalDateTime publishedAt;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
