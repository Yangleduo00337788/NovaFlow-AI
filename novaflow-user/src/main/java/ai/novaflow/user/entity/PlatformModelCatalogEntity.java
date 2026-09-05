package ai.novaflow.user.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Table("platform_model_catalog")
public class PlatformModelCatalogEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private String providerCode;
    private String modelName;
    private String displayName;
    private String modelType;
    @Column("input_price_per_1k")
    private BigDecimal inputPricePer1k;
    @Column("output_price_per_1k")
    private BigDecimal outputPricePer1k;
    private String currency;
    private Integer enabled;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
