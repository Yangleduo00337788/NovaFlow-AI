package ai.novaflow.model.service;

import ai.novaflow.common.event.TokenUsageRecordedEvent;
import ai.novaflow.model.domain.BillingCurrency;
import ai.novaflow.model.domain.ModelPriceCatalog;
import ai.novaflow.model.domain.ModelUsageAggregate;
import ai.novaflow.model.domain.dto.ModelUsageRecordRequest;
import ai.novaflow.model.domain.vo.ModelCostSummaryVO;
import ai.novaflow.model.domain.vo.ModelUsageItemVO;
import ai.novaflow.model.domain.vo.ModelUsageStatsVO;
import ai.novaflow.model.entity.ModelConfigEntity;
import ai.novaflow.model.entity.ModelProviderEntity;
import ai.novaflow.model.entity.TokenUsageEntity;
import ai.novaflow.model.mapper.ModelConfigMapper;
import ai.novaflow.model.mapper.ModelProviderMapper;
import ai.novaflow.model.mapper.TokenUsageMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelUsageService {

    private final TokenUsageMapper tokenUsageMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final ModelProviderMapper modelProviderMapper;
    private final ApplicationEventPublisher eventPublisher;

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

        CostCalculation calculation = calculateCostWithCurrency(
                request.getModelConfigId(), inputTokens, outputTokens, totalTokens);

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
        entity.setCost(calculation.cost());
        entity.setCurrency(calculation.currency().getCode());
        entity.setLatencyMs(request.getLatencyMs() != null ? request.getLatencyMs().intValue() : null);
        entity.setSuccess(request.getSuccess() == null || Boolean.TRUE.equals(request.getSuccess()));
        entity.setErrorMessage(truncate(request.getErrorMessage(), 512));
        entity.setTraceId(StringUtils.hasText(request.getTraceId()) ? request.getTraceId().trim() : null);
        entity.setUsageDate(LocalDate.now());
        entity.setCreatedAt(LocalDateTime.now());
        tokenUsageMapper.insert(entity);
        eventPublisher.publishEvent(new TokenUsageRecordedEvent(request.getTenantId()));
    }

    public ModelUsageStatsVO getUsageStats(Long tenantId) {
        backfillMissingCosts(tenantId);

        Long totalCalls = tokenUsageMapper.countCallsByTenant(tenantId);
        Long totalTokens = tokenUsageMapper.sumTokensByTenant(tenantId);
        List<ModelCostSummaryVO> costSummaries = buildCostSummaries(tenantId);
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
                .totalCost(formatCombinedCost(costSummaries))
                .costSummaries(costSummaries)
                .topModels(topModels)
                .build();
    }

    private List<ModelCostSummaryVO> buildCostSummaries(Long tenantId) {
        List<ModelCostSummaryVO> summaries = new ArrayList<>();
        appendCostSummary(summaries, tenantId, BillingCurrency.CNY);
        appendCostSummary(summaries, tenantId, BillingCurrency.USD);
        return summaries;
    }

    private void appendCostSummary(List<ModelCostSummaryVO> summaries, Long tenantId, BillingCurrency currency) {
        BigDecimal amount = tokenUsageMapper.sumCostByTenantAndCurrency(tenantId, currency.getCode());
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        summaries.add(ModelCostSummaryVO.builder()
                .currency(currency.getCode())
                .symbol(currency.getSymbol())
                .amount(formatAmount(amount))
                .build());
    }

    private String formatCombinedCost(List<ModelCostSummaryVO> summaries) {
        if (summaries.isEmpty()) {
            return "¥0.00";
        }
        return summaries.stream()
                .map(item -> item.getSymbol() + item.getAmount())
                .reduce((left, right) -> left + " + " + right)
                .orElse("¥0.00");
    }

    private CostCalculation calculateCostWithCurrency(Long modelConfigId, int inputTokens, int outputTokens, int totalTokens) {
        ModelConfigEntity config = modelConfigMapper.selectOneByQuery(
                QueryWrapper.create().eq("id", modelConfigId).eq("is_deleted", 0)
        );
        if (config == null) {
            return new CostCalculation(BigDecimal.ZERO, BillingCurrency.CNY);
        }

        ModelProviderEntity provider = modelProviderMapper.selectOneById(config.getProviderId());
        BillingCurrency currency = provider != null
                ? BillingCurrency.fromProviderCode(provider.getProviderCode())
                : BillingCurrency.CNY;

        int effectiveInput = inputTokens;
        int effectiveOutput = outputTokens;
        if (effectiveInput == 0 && effectiveOutput == 0 && totalTokens > 0) {
            effectiveInput = totalTokens / 2;
            effectiveOutput = totalTokens - effectiveInput;
        }

        BigDecimal inputPrice = config.getInputPrice();
        BigDecimal outputPrice = config.getOutputPrice();
        if (inputPrice == null || outputPrice == null) {
            var catalogPrice = ModelPriceCatalog.resolve(
                    provider != null ? provider.getProviderCode() : null,
                    config.getModelName()
            );
            if (catalogPrice.isPresent()) {
                if (inputPrice == null) {
                    inputPrice = catalogPrice.get().inputPer1k();
                }
                if (outputPrice == null) {
                    outputPrice = catalogPrice.get().outputPer1k();
                }
                currency = catalogPrice.get().currency();
            }
        }

        if (inputPrice == null || outputPrice == null) {
            return new CostCalculation(BigDecimal.ZERO, currency);
        }

        BigDecimal cost = BigDecimal.ZERO;
        if (effectiveInput > 0) {
            cost = cost.add(inputPrice
                    .multiply(BigDecimal.valueOf(effectiveInput))
                    .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP));
        }
        if (effectiveOutput > 0) {
            cost = cost.add(outputPrice
                    .multiply(BigDecimal.valueOf(effectiveOutput))
                    .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP));
        }
        return new CostCalculation(cost, currency);
    }

    private void backfillMissingCosts(Long tenantId) {
        List<TokenUsageEntity> records = tokenUsageMapper.selectListByQuery(
                QueryWrapper.create().eq("tenant_id", tenantId)
        );
        for (TokenUsageEntity record : records) {
            CostCalculation calculation = calculateCostWithCurrency(
                    record.getModelConfigId(),
                    safeInt(record.getInputTokens()),
                    safeInt(record.getOutputTokens()),
                    safeInt(record.getTotalTokens())
            );
            boolean needsUpdate = record.getCost() == null
                    || record.getCost().compareTo(BigDecimal.ZERO) == 0
                    || !StringUtils.hasText(record.getCurrency())
                    || !calculation.currency().getCode().equalsIgnoreCase(record.getCurrency());
            if (!needsUpdate && calculation.cost().compareTo(record.getCost()) == 0) {
                continue;
            }
            if (calculation.cost().compareTo(BigDecimal.ZERO) > 0 || needsUpdate) {
                record.setCost(calculation.cost());
                record.setCurrency(calculation.currency().getCode());
                tokenUsageMapper.update(record);
            }
        }
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String formatAmount(BigDecimal cost) {
        if (cost == null) {
            return "0.00";
        }
        if (cost.compareTo(new BigDecimal("0.01")) < 0 && cost.compareTo(BigDecimal.ZERO) > 0) {
            return cost.setScale(4, RoundingMode.HALF_UP).toPlainString();
        }
        return cost.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private record CostCalculation(BigDecimal cost, BillingCurrency currency) {
    }
}
