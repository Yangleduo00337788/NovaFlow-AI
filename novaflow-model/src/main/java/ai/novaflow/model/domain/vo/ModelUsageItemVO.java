package ai.novaflow.model.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ModelUsageItemVO {

    private String modelName;
    private String displayName;
    private Long calls;
    private Long tokens;
}
