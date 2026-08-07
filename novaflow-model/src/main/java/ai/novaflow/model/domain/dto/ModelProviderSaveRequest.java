package ai.novaflow.model.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ModelProviderSaveRequest {

    @NotBlank(message = "提供商标识不能为空")
    private String providerCode;

    private String baseUrl;

    private String apiKey;

    private Boolean enabled;
}
