package ai.novaflow.model.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmbeddingOptionVO {

    private String modelName;
    private String displayName;
    private String providerCode;
    private String providerName;
    private Boolean configured;
}
