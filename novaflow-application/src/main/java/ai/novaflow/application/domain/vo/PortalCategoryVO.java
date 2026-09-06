package ai.novaflow.application.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PortalCategoryVO {

    private String code;
    private String label;
    private int appCount;
}
