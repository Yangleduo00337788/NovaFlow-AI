package ai.novaflow.user.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("user_notification")
public class UserNotificationEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private Long userId;
    private String category;
    private String title;
    private String content;
    private String linkUrl;
    private Integer isRead;
    private LocalDateTime createdAt;
}
