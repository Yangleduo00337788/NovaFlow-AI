package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PlatformModelCatalogVO {

    private Long id;
    private String providerCode;
    private String modelName;
    private String displayName;
    private String modelType;
    private BigDecimal inputPricePer1k;
    private BigDecimal outputPricePer1k;
    private String currency;
    private Boolean enabled;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
