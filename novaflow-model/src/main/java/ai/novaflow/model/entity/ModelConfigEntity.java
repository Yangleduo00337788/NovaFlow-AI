package ai.novaflow.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Table("model_config")
public class ModelConfigEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private Long providerId;
    private String modelName;
    private String modelType;
    private String displayName;
    private Integer contextWindow;
    private Integer maxOutputTokens;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private BigDecimal defaultTemperature;
    private Integer isEnabled;
    private Integer isDefault;
    private String config;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
