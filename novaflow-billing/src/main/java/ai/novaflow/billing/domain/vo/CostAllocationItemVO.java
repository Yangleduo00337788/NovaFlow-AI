package ai.novaflow.billing.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CostAllocationItemVO {

    private Long id;
    private String name;
    private long calls;
    private long tokens;
    private int tokenPercent;
    private String costLabel;
}
