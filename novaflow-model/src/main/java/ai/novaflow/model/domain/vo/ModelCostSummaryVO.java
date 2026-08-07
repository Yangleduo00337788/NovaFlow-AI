package ai.novaflow.model.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ModelCostSummaryVO {

    private String currency;
    private String symbol;
    private String amount;
}
