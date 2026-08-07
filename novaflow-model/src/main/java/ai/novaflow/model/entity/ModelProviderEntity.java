package ai.novaflow.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("model_provider")
public class ModelProviderEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private String providerCode;
    private String providerName;
    private String baseUrl;
    private String apiKeyEncrypted;
    private Integer isEnabled;
    private String config;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
