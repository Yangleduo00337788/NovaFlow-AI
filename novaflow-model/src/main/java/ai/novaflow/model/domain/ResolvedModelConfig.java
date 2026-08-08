package ai.novaflow.model.domain;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ResolvedModelConfig {

    private Long modelConfigId;
    private String providerCode;
    private String providerName;
    private String modelName;
    private String displayName;
    private String baseUrl;
    private String apiKey;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Boolean enableDeepThinking;
    private Boolean enableWebSearch;
}
