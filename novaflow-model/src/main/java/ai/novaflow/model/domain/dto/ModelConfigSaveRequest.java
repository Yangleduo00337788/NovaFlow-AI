package ai.novaflow.model.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ModelConfigSaveRequest {

    @NotNull(message = "提供商不能为空")
    private Long providerId;

    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    @NotBlank(message = "模型类型不能为空")
    private String modelType;

    @NotBlank(message = "显示名称不能为空")
    private String displayName;

    private Integer contextWindow;
    private Integer maxOutputTokens;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private BigDecimal defaultTemperature;
    private Boolean enabled;
    private Boolean isDefault;
}
