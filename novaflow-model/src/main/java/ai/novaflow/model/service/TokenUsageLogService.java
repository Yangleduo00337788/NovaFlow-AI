package ai.novaflow.model.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.model.domain.BillingCurrency;
import ai.novaflow.model.domain.TokenUsageLogRow;
import ai.novaflow.model.domain.vo.TokenUsageLogVO;
import ai.novaflow.model.mapper.TokenUsageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TokenUsageLogService {

    private final TokenUsageMapper tokenUsageMapper;

    public PageResult<TokenUsageLogVO> page(int page, int pageSize, Long agentId, String keyword, Boolean success) {
        Long tenantId = requireTenantId();
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Integer successFilter = resolveSuccessFilter(success);
        int offset = (safePage - 1) * safePageSize;

        Long total = tokenUsageMapper.countLogs(tenantId, agentId, null, null, null, trimmedKeyword, successFilter);
        List<TokenUsageLogVO> list = tokenUsageMapper.pageLogs(
                        tenantId, agentId, null, null, null, trimmedKeyword, successFilter, offset, safePageSize)
                .stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(list, total != null ? total : 0L, safePage, safePageSize);
    }

    private TokenUsageLogVO toVO(TokenUsageLogRow row) {
        BillingCurrency currency = BillingCurrency.fromCode(row.getCurrency());
        boolean successful = isSuccessful(row.getSuccess());
        return TokenUsageLogVO.builder()
                .id(row.getId())
                .agentId(row.getAgentId())
                .agentName(StringUtils.hasText(row.getAgentName()) ? row.getAgentName() : "未知 Agent")
                .modelName(row.getModelName())
                .displayName(StringUtils.hasText(row.getDisplayName()) ? row.getDisplayName() : row.getModelName())
                .usageType(row.getUsageType())
                .inputTokens(row.getInputTokens())
                .outputTokens(row.getOutputTokens())
                .totalTokens(row.getTotalTokens())
                .cost(row.getCost())
                .currency(currency.getCode())
                .costLabel(formatCost(row.getCost(), currency))
                .latencyMs(row.getLatencyMs())
                .success(successful)
                .statusLabel(successful ? "成功" : "失败")
                .userId(row.getUserId())
                .createdAt(row.getCreatedAt())
                .build();
    }

    private Integer resolveSuccessFilter(Boolean success) {
        if (success == null) {
            return null;
        }
        return success ? 1 : 0;
    }

    private boolean isSuccessful(Integer success) {
        return success == null || success != 0;
    }

    private String formatCost(BigDecimal cost, BillingCurrency currency) {
        if (cost == null || cost.compareTo(BigDecimal.ZERO) <= 0) {
            return currency.getSymbol() + "0.00";
        }
        return currency.getSymbol() + cost.setScale(cost.compareTo(new BigDecimal("0.01")) < 0 ? 4 : 2, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户上下文缺失");
        }
        return tenantId;
    }
}
