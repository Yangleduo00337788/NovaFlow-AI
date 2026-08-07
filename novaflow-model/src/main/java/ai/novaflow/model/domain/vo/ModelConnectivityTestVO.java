package ai.novaflow.model.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ModelConnectivityTestVO {

    private Boolean success;
    private String message;
    private Long latencyMs;
    private String modelName;
}
