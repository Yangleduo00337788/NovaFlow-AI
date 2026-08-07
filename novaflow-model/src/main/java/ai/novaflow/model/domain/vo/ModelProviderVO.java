package ai.novaflow.model.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ModelProviderVO {

    private Long id;
    private String providerCode;
    private String providerName;
    private String description;
    private String baseUrl;
    private String defaultBaseUrl;
    private String apiKeyMasked;
    private Boolean configured;
    private Boolean enabled;
    private Integer modelCount;
    private LocalDateTime updatedAt;
}
