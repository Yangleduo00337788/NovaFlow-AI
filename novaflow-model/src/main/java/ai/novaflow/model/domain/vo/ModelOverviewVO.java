package ai.novaflow.model.domain.vo;

import ai.novaflow.model.domain.vo.ModelCostSummaryVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ModelOverviewVO {

    private Long totalCalls;
    private Long totalTokens;
    private String totalCost;
    private List<ModelCostSummaryVO> costSummaries;
    private Long configuredProviders;
    private Long enabledModels;
    private List<ModelUsageItemVO> topModels;
}
