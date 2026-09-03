package ai.novaflow.tenant.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("tenant_notify_channel")
public class TenantNotifyChannelEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private Integer emailEnabled;
    private String emailRecipients;
    private Integer webhookEnabled;
    private String webhookUrl;
    private String webhookSecret;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
