package ai.novaflow.user.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("role")
public class RoleEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private String roleCode;
    private String roleName;
    private Integer isSystem;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
