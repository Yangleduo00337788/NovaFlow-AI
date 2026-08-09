package ai.novaflow.billing.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BillingUsageTypeVO {

    private String usageType;
    private String usageTypeLabel;
    private Long calls;
    private Long tokens;
}
