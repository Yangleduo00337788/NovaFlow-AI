package ai.novaflow.billing.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CostAllocationVO {

    private String periodLabel;
    private String dimension;
    private String dimensionLabel;
    private long totalCalls;
    private long totalTokens;
    private String totalCostLabel;
    private List<CostAllocationItemVO> items;
}
