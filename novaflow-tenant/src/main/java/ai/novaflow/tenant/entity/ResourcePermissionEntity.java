package ai.novaflow.tenant.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("resource_permission")
public class ResourcePermissionEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private String resourceType;
    private Long resourceId;
    private Long userId;
    private String permissionCode;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
