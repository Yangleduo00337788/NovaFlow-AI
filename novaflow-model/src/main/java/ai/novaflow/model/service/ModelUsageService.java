package ai.novaflow.model.service;

import ai.novaflow.model.domain.ModelUsageAggregate;
import ai.novaflow.model.domain.dto.ModelUsageRecordRequest;
import ai.novaflow.model.domain.vo.ModelUsageStatsVO;
import ai.novaflow.model.domain.vo.ModelUsageItemVO;
import ai.novaflow.model.entity.ModelConfigEntity;
import ai.novaflow.model.entity.TokenUsageEntity;
import ai.novaflow.model.mapper.ModelConfigMapper;
import ai.novaflow.model.mapper.TokenUsageMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelUsageService {

    private final TokenUsageMapper tokenUsageMapper;
    private final ModelConfigMapper modelConfigMapper;

    @Transactional
    public void record(ModelUsageRecordRequest request) {
        if (request.getTenantId() == null || request.getModelConfigId() == null) {
            return;
        }

        int inputTokens = safeInt(request.getInputTokens());
        int outputTokens = safeInt(request.getOutputTokens());
        int totalTokens = request.getTotalTokens() != null && request.getTotalTokens() > 0
                ? request.getTotalTokens()
                : inputTokens + outputTokens;

        TokenUsageEntity entity = new TokenUsageEntity();
        entity.setTenantId(request.getTenantId());
        entity.setApplicationId(request.getApplicationId());
        entity.setAgentId(request.getAgentId());
        entity.setUserId(request.getUserId());
        entity.setModelConfigId(request.getModelConfigId());
        entity.setUsageType(request.getUsageType() != null ? request.getUsageType() : "chat");
        entity.setInputTokens(inputTokens);
        entity.setOutputTokens(outputTokens);
        entity.setTotalTokens(totalTokens);
        entity.setCost(calculateCost(request.getModelConfigId(), inputTokens, outputTokens));
        entity.setLatencyMs(request.getLatencyMs() != null ? request.getLatencyMs().intValue() : null);
        entity.setUsageDate(LocalDate.now());
        entity.setCreatedAt(LocalDateTime.now());
        tokenUsageMapper.insert(entity);
    }

    public ModelUsageStatsVO getUsageStats(Long tenantId) {
        Long totalCalls = tokenUsageMapper.countCallsByTenant(tenantId);
        Long totalTokens = tokenUsageMapper.sumTokensByTenant(tenantId);
        BigDecimal totalCost = tokenUsageMapper.sumCostByTenant(tenantId);
        List<ModelUsageAggregate> aggregates = tokenUsageMapper.topModelsByTenant(tenantId);

        List<ModelUsageItemVO> topModels = aggregates.stream()
                .map(item -> ModelUsageItemVO.builder()
                        .modelName(item.getModelName())
                        .displayName(item.getDisplayName())
                        .calls(item.getCalls())
                        .tokens(item.getTokens())
                        .build())
                .toList();

        return ModelUsageStatsVO.builder()
                .totalCalls(totalCalls != null ? totalCalls : 0L)
                .totalTokens(totalTokens != null ? totalTokens : 0L)
                .totalCost(formatCost(totalCost))
                .topModels(topModels)
                .build();
    }

    private BigDecimal calculateCost(Long modelConfigId, int inputTokens, int outputTokens) {
        ModelConfigEntity config = modelConfigMapper.selectOneByQuery(
                QueryWrapper.create().eq("id", modelConfigId).eq("is_deleted", 0)
        );
        if (config == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal cost = BigDecimal.ZERO;
        if (config.getInputPrice() != null && inputTokens > 0) {
            cost = cost.add(config.getInputPrice()
                    .multiply(BigDecimal.valueOf(inputTokens))
                    .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP));
        }
        if (config.getOutputPrice() != null && outputTokens > 0) {
            cost = cost.add(config.getOutputPrice()
                    .multiply(BigDecimal.valueOf(outputTokens))
                    .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP));
        }
        return cost;
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private String formatCost(BigDecimal cost) {
        if (cost == null) {
            return "0.00";
        }
        return cost.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
