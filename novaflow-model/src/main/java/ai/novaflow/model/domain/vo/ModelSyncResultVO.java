package ai.novaflow.model.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ModelSyncResultVO {

    private Integer added;
    private Integer updated;
    private Integer disabled;
    private Integer total;
    private String message;
}
