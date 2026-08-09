package ai.novaflow.user.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("workspace")
public class WorkspaceEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private String workspaceName;
    private String description;
    private String icon;
    private Integer isDefault;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
