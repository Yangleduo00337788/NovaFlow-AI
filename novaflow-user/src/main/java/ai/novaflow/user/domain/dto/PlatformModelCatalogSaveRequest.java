package ai.novaflow.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlatformModelCatalogSaveRequest {

    @NotBlank(message = "供应商编码不能为空")
    private String providerCode;

    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    private String displayName;
    private String modelType;
    private BigDecimal inputPricePer1k;
    private BigDecimal outputPricePer1k;
    private String currency;
    private Integer enabled;
    private String description;
}
