package ai.novaflow.user.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("permission")
public class PermissionEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private String permissionCode;
    private String permissionName;
    private String module;
    private String description;
    private LocalDateTime createdAt;
}
