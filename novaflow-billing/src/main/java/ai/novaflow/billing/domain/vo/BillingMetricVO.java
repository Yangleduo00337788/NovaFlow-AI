package ai.novaflow.billing.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BillingMetricVO {

    private String key;
    private String label;
    private String value;
    private String hint;
}
