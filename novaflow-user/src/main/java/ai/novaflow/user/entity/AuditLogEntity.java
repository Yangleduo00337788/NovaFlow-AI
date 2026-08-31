package ai.novaflow.user.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("audit_log")
public class AuditLogEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private Long userId;
    private String action;
    private String resourceType;
    private Long resourceId;
    private String detail;
    private String clientIp;
    private LocalDateTime createdAt;
}
