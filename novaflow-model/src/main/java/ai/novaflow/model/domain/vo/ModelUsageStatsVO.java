package ai.novaflow.model.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ModelUsageStatsVO {

    private Long totalCalls;
    private Long totalTokens;
    private String totalCost;
    private List<ModelCostSummaryVO> costSummaries;
    private List<ModelUsageItemVO> topModels;
}
