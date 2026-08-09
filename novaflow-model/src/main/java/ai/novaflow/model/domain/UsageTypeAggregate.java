package ai.novaflow.model.domain;

import lombok.Data;

@Data
public class UsageTypeAggregate {

    private String usageType;
    private Long calls;
    private Long tokens;
}
