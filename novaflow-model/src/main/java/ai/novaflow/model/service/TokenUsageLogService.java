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
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TokenUsageLogService {

    private final TokenUsageMapper tokenUsageMapper;

    public PageResult<TokenUsageLogVO> page(
            int page, int pageSize, Long agentId, String keyword, Boolean success, String usageType) {
        Long tenantId = requireTenantId();
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String trimmedUsageType = StringUtils.hasText(usageType) ? usageType.trim() : null;
        Integer successFilter = resolveSuccessFilter(success);
        int offset = (safePage - 1) * safePageSize;

        Long total = tokenUsageMapper.countLogs(
                tenantId, agentId, trimmedUsageType, null, null, trimmedKeyword, successFilter);
        List<TokenUsageLogVO> list = tokenUsageMapper.pageLogs(
                        tenantId, agentId, trimmedUsageType, null, null, trimmedKeyword, successFilter, offset, safePageSize)
                .stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(list, total != null ? total : 0L, safePage, safePageSize);
    }

    public byte[] exportCsv(Long agentId, String keyword, Boolean success, String usageType) {
        Long tenantId = requireTenantId();
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String trimmedUsageType = StringUtils.hasText(usageType) ? usageType.trim() : null;
        Integer successFilter = resolveSuccessFilter(success);
        List<TokenUsageLogRow> rows = tokenUsageMapper.pageLogs(
                tenantId, agentId, trimmedUsageType, null, null, trimmedKeyword, successFilter, 0, 5000);
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("时间,状态,Agent,模型,类型,输入Tokens,输出Tokens,总Tokens,耗时(ms),成本,Trace ID,错误信息\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (TokenUsageLogRow row : rows) {
            TokenUsageLogVO vo = toVO(row);
            csv.append(csvCell(vo.getCreatedAt() != null ? vo.getCreatedAt().format(formatter) : "")).append(',')
                    .append(csvCell(vo.getStatusLabel())).append(',')
                    .append(csvCell(vo.getAgentName())).append(',')
                    .append(csvCell(vo.getDisplayName())).append(',')
                    .append(csvCell(vo.getUsageType())).append(',')
                    .append(vo.getInputTokens() != null ? vo.getInputTokens() : 0).append(',')
                    .append(vo.getOutputTokens() != null ? vo.getOutputTokens() : 0).append(',')
                    .append(vo.getTotalTokens() != null ? vo.getTotalTokens() : 0).append(',')
                    .append(vo.getLatencyMs() != null ? vo.getLatencyMs() : 0).append(',')
                    .append(csvCell(vo.getCostLabel())).append(',')
                    .append(csvCell(vo.getTraceId())).append(',')
                    .append(csvCell(vo.getErrorMessage()))
                    .append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csvCell(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
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
                .errorMessage(row.getErrorMessage())
                .traceId(row.getTraceId())
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
