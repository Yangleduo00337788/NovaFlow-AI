package ai.novaflow.billing.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BillingTrendPointVO {

    private String label;
    private Long tokens;
}
