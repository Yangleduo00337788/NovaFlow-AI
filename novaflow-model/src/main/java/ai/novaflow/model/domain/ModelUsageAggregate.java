package ai.novaflow.model.domain;

import lombok.Data;

@Data
public class ModelUsageAggregate {

    private String modelName;
    private String displayName;
    private Long calls;
    private Long tokens;
}
