package ai.novaflow.billing.service;

import ai.novaflow.billing.domain.vo.BillingMetricVO;
import ai.novaflow.billing.domain.vo.BillingModelUsageVO;
import ai.novaflow.billing.domain.vo.BillingOverviewVO;
import ai.novaflow.billing.domain.vo.BillingQuotaVO;
import ai.novaflow.billing.domain.vo.BillingTrendPointVO;
import ai.novaflow.billing.domain.vo.BillingUsageTypeVO;
import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.model.domain.BillingCurrency;
import ai.novaflow.model.domain.ModelUsageAggregate;
import ai.novaflow.model.domain.TokenUsageLogRow;
import ai.novaflow.model.domain.UsageTrendPoint;
import ai.novaflow.model.domain.UsageTypeAggregate;
import ai.novaflow.model.domain.vo.TokenUsageLogVO;
import ai.novaflow.model.mapper.TokenUsageMapper;
import ai.novaflow.user.entity.TenantEntity;
import ai.novaflow.user.mapper.TenantMapper;
import ai.novaflow.user.mapper.TenantMemberMapper;
import ai.novaflow.user.service.PermissionService;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final TokenUsageMapper tokenUsageMapper;
    private final TenantMapper tenantMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final PermissionService permissionService;

    public BillingOverviewVO getOverview(String month) {
        requireBillingViewPermission();
        Long tenantId = requireTenantId();
        YearMonth current = resolveMonth(month);
        YearMonth previous = current.minusMonths(1);
        LocalDate currentStart = current.atDay(1);
        LocalDate currentEnd = current.atEndOfMonth();
        LocalDate previousStart = previous.atDay(1);
        LocalDate previousEnd = previous.atEndOfMonth();

        long totalCalls = safeLong(tokenUsageMapper.countCallsBetween(tenantId, currentStart, currentEnd));
        long totalTokens = safeLong(tokenUsageMapper.sumTokensBetween(tenantId, currentStart, currentEnd));
        long prevCalls = safeLong(tokenUsageMapper.countCallsBetween(tenantId, previousStart, previousEnd));
        long prevTokens = safeLong(tokenUsageMapper.sumTokensBetween(tenantId, previousStart, previousEnd));

        BigDecimal cnyCost = safeDecimal(tokenUsageMapper.sumCostBetween(tenantId, BillingCurrency.CNY.getCode(), currentStart, currentEnd));
        BigDecimal usdCost = safeDecimal(tokenUsageMapper.sumCostBetween(tenantId, BillingCurrency.USD.getCode(), currentStart, currentEnd));

        List<BillingTrendPointVO> dailyTrend = tokenUsageMapper.dailyTokenTrend(tenantId, currentStart, currentEnd)
                .stream()
                .map(point -> BillingTrendPointVO.builder()
                        .label(point.getLabel())
                        .tokens(safeLong(point.getValue()))
                        .build())
                .toList();

        List<BillingUsageTypeVO> usageByType = tokenUsageMapper.usageByType(tenantId, currentStart, currentEnd)
                .stream()
                .map(this::toUsageTypeVO)
                .toList();

        List<BillingModelUsageVO> topModels = tokenUsageMapper.topModelsBetween(tenantId, currentStart, currentEnd, 5)
                .stream()
                .map(this::toModelUsageVO)
                .toList();

        return BillingOverviewVO.builder()
                .periodLabel(current.format(DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)))
                .totalCalls(totalCalls)
                .totalTokens(totalTokens)
                .totalCostLabel(formatCombinedCost(cnyCost, usdCost))
                .tokenChangePercent(formatChangePercent(totalTokens, prevTokens))
                .callChangePercent(formatChangePercent(totalCalls, prevCalls))
                .metrics(buildMetrics(totalCalls, totalTokens, formatCombinedCost(cnyCost, usdCost)))
                .dailyTrend(dailyTrend)
                .usageByType(usageByType)
                .topModels(topModels)
                .quota(buildQuota(tenantId, currentStart, currentEnd))
                .build();
    }

    public BillingQuotaVO getQuota() {
        requireBillingViewPermission();
        Long tenantId = requireTenantId();
        YearMonth current = YearMonth.now();
        return buildQuota(tenantId, current.atDay(1), current.atEndOfMonth());
    }

    public PageResult<TokenUsageLogVO> pageRecords(
            int page,
            int pageSize,
            Long agentId,
            String usageType,
            String month,
            String keyword) {
        requireBillingViewPermission();
        Long tenantId = requireTenantId();
        YearMonth period = resolveMonth(month);
        LocalDate startDate = period.atDay(1);
        LocalDate endDate = period.atEndOfMonth();

        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String trimmedUsageType = StringUtils.hasText(usageType) ? usageType.trim() : null;
        int offset = (safePage - 1) * safePageSize;

        Long total = tokenUsageMapper.countLogs(
                tenantId, agentId, trimmedUsageType, startDate, endDate, trimmedKeyword);
        List<TokenUsageLogVO> list = tokenUsageMapper.pageLogs(
                        tenantId, agentId, trimmedUsageType, startDate, endDate, trimmedKeyword, offset, safePageSize)
                .stream()
                .map(this::toLogVO)
                .toList();
        return PageResult.of(list, total != null ? total : 0L, safePage, safePageSize);
    }

    private BillingQuotaVO buildQuota(Long tenantId, LocalDate startDate, LocalDate endDate) {
        TenantEntity tenant = getTenantOrThrow(tenantId);
        int memberCount = countActiveMembers(tenantId);
        int maxMembers = tenant.getMaxMembers() != null && tenant.getMaxMembers() > 0 ? tenant.getMaxMembers() : 100;
        long usedTokens = safeLong(tokenUsageMapper.sumTokensBetween(tenantId, startDate, endDate));
        long monthlyQuota = tenant.getMonthlyTokenQuota() != null ? tenant.getMonthlyTokenQuota() : 0L;

        return BillingQuotaVO.builder()
                .planType(tenant.getPlanType())
                .planTypeLabel(resolvePlanTypeLabel(tenant.getPlanType()))
                .expireAt(tenant.getExpireAt())
                .monthlyTokenQuota(monthlyQuota > 0 ? monthlyQuota : null)
                .usedTokens(usedTokens)
                .tokenUsedPercent(calcPercent(usedTokens, monthlyQuota))
                .memberCount(memberCount)
                .maxMembers(maxMembers)
                .memberUsedPercent(calcPercent(memberCount, maxMembers))
                .maxAgents(tenant.getMaxAgents())
                .maxKnowledge(tenant.getMaxKnowledge())
                .build();
    }

    private List<BillingMetricVO> buildMetrics(long calls, long tokens, String costLabel) {
        List<BillingMetricVO> metrics = new ArrayList<>();
        metrics.add(BillingMetricVO.builder().key("calls").label("本月调用").value(formatCount(calls)).hint("次").build());
        metrics.add(BillingMetricVO.builder().key("tokens").label("本月 Token").value(formatCount(tokens)).hint("tokens").build());
        metrics.add(BillingMetricVO.builder().key("cost").label("本月预估费用").value(costLabel).hint("按模型单价估算").build());
        return metrics;
    }

    private BillingUsageTypeVO toUsageTypeVO(UsageTypeAggregate item) {
        return BillingUsageTypeVO.builder()
                .usageType(item.getUsageType())
                .usageTypeLabel(resolveUsageTypeLabel(item.getUsageType()))
                .calls(safeLong(item.getCalls()))
                .tokens(safeLong(item.getTokens()))
                .build();
    }

    private BillingModelUsageVO toModelUsageVO(ModelUsageAggregate item) {
        return BillingModelUsageVO.builder()
                .modelName(item.getModelName())
                .displayName(StringUtils.hasText(item.getDisplayName()) ? item.getDisplayName() : item.getModelName())
                .calls(safeLong(item.getCalls()))
                .tokens(safeLong(item.getTokens()))
                .build();
    }

    private TokenUsageLogVO toLogVO(TokenUsageLogRow row) {
        BillingCurrency currency = BillingCurrency.fromCode(row.getCurrency());
        return TokenUsageLogVO.builder()
                .id(row.getId())
                .agentId(row.getAgentId())
                .agentName(StringUtils.hasText(row.getAgentName()) ? row.getAgentName() : "系统调用")
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
                .userId(row.getUserId())
                .createdAt(row.getCreatedAt())
                .build();
    }

    private YearMonth resolveMonth(String month) {
        if (!StringUtils.hasText(month)) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (Exception e) {
            throw new BusinessException("月份格式无效，请使用 yyyy-MM");
        }
    }

    private String resolvePlanTypeLabel(String planType) {
        if (!StringUtils.hasText(planType)) {
            return "免费版";
        }
        return switch (planType.trim().toLowerCase(Locale.ROOT)) {
            case "free" -> "免费版";
            case "pro" -> "专业版";
            case "enterprise" -> "企业版";
            default -> planType;
        };
    }

    private String resolveUsageTypeLabel(String usageType) {
        if (!StringUtils.hasText(usageType)) {
            return "对话";
        }
        return switch (usageType.trim().toLowerCase(Locale.ROOT)) {
            case "chat" -> "对话";
            case "workflow" -> "工作流";
            case "rag" -> "RAG";
            default -> usageType;
        };
    }

    private String formatCombinedCost(BigDecimal cnyCost, BigDecimal usdCost) {
        List<String> parts = new ArrayList<>();
        if (cnyCost.compareTo(BigDecimal.ZERO) > 0) {
            parts.add(BillingCurrency.CNY.getSymbol() + formatAmount(cnyCost));
        }
        if (usdCost.compareTo(BigDecimal.ZERO) > 0) {
            parts.add(BillingCurrency.USD.getSymbol() + formatAmount(usdCost));
        }
        if (parts.isEmpty()) {
            return BillingCurrency.CNY.getSymbol() + "0.00";
        }
        return String.join(" + ", parts);
    }

    private String formatCost(BigDecimal cost, BillingCurrency currency) {
        if (cost == null || cost.compareTo(BigDecimal.ZERO) <= 0) {
            return currency.getSymbol() + "0.00";
        }
        return currency.getSymbol() + formatAmount(cost);
    }

    private String formatAmount(BigDecimal cost) {
        if (cost.compareTo(new BigDecimal("0.01")) < 0) {
            return cost.setScale(4, RoundingMode.HALF_UP).toPlainString();
        }
        return cost.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatCount(long value) {
        if (value >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0);
        }
        if (value >= 10_000) {
            return String.format(Locale.ROOT, "%.1f万", value / 10_000.0);
        }
        return String.valueOf(value);
    }

    private String formatChangePercent(long current, long previous) {
        if (previous <= 0) {
            return current > 0 ? "+100%" : "0%";
        }
        double change = (current - previous) * 100.0 / previous;
        return String.format(Locale.ROOT, "%s%.1f%%", change >= 0 ? "+" : "", change);
    }

    private Integer calcPercent(long used, long limit) {
        if (limit <= 0) {
            return null;
        }
        return (int) Math.min(100, Math.round(used * 100.0 / limit));
    }

    private long safeLong(Long value) {
        return value != null ? value : 0L;
    }

    private BigDecimal safeDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private TenantEntity getTenantOrThrow(Long tenantId) {
        TenantEntity tenant = tenantMapper.selectOneByQuery(
                QueryWrapper.create().eq("id", tenantId).eq("is_deleted", 0)
        );
        if (tenant == null) {
            throw new BusinessException("企业不存在");
        }
        return tenant;
    }

    private int countActiveMembers(Long tenantId) {
        return (int) tenantMemberMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("status", 1)
                        .eq("is_deleted", 0)
        );
    }

    private void requireBillingViewPermission() {
        permissionService.requireAnyPermission(
                StpUtil.getLoginIdAsLong(),
                requireTenantId(),
                "billing:view",
                "billing:manage"
        );
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户上下文缺失");
        }
        return tenantId;
    }
}
