package ai.novaflow.model.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpstreamModelDescriptor {

    private String modelName;
    private String modelType;
    private String displayName;
}
