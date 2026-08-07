package ai.novaflow.model.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ModelConfigVO {

    private Long id;
    private Long providerId;
    private String providerCode;
    private String providerName;
    private String modelName;
    private String modelType;
    private String displayName;
    private Integer contextWindow;
    private Integer maxOutputTokens;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private BigDecimal defaultTemperature;
    private Boolean enabled;
    private Boolean isDefault;
    private LocalDateTime updatedAt;
}
