package ai.novaflow.user.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("user_favorite")
public class UserFavoriteEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private Long userId;
    private String resourceType;
    private Long resourceId;
    private String resourceName;
    private LocalDateTime createdAt;
}
