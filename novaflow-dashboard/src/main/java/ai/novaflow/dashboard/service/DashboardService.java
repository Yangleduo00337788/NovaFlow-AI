package ai.novaflow.dashboard.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.dashboard.domain.DashboardOverviewVO;
import ai.novaflow.dashboard.domain.NamedCountRow;
import ai.novaflow.dashboard.domain.RecentUsageLogRow;
import ai.novaflow.dashboard.domain.TrendPointRow;
import ai.novaflow.dashboard.mapper.DashboardStatsMapper;
import ai.novaflow.model.domain.ModelUsageAggregate;
import ai.novaflow.model.mapper.TokenUsageMapper;
import ai.novaflow.user.entity.UserRecentAccessEntity;
import ai.novaflow.user.service.RecentAccessService;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardStatsMapper dashboardStatsMapper;
    private final TokenUsageMapper tokenUsageMapper;
    private final RecentAccessService recentAccessService;

    public DashboardOverviewVO getOverview() {
        Long tenantId = requireTenantId();
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;

        long appCount = safeLong(dashboardStatsMapper.countApplications(tenantId));
        long agentCount = safeLong(dashboardStatsMapper.countAgents(tenantId));
        long kbCount = safeLong(dashboardStatsMapper.countKnowledgeBases(tenantId));
        long totalCalls = safeLong(tokenUsageMapper.countCallsByTenant(tenantId));
        long totalTokens = safeLong(tokenUsageMapper.sumTokensByTenant(tenantId));
        BigDecimal totalCost = tokenUsageMapper.sumCostByTenantAndCurrency(tenantId, "CNY");

        long calls7d = safeLong(dashboardStatsMapper.countCallsLast7Days(tenantId));
        long callsPrev7d = safeLong(dashboardStatsMapper.countCallsPrev7Days(tenantId));
        long tokens7d = safeLong(dashboardStatsMapper.sumTokensLast7Days(tenantId));
        long tokensPrev7d = safeLong(dashboardStatsMapper.sumTokensPrev7Days(tenantId));

        List<ModelUsageAggregate> modelAggregates = tokenUsageMapper.topModelsByTenant(tenantId);
        long modelTokenSum = modelAggregates.stream().mapToLong(item -> safeLong(item.getTokens())).sum();

        return DashboardOverviewVO.builder()
                .stats(List.of(
                        card("apps", "应用总数", formatCount(appCount), "—", true),
                        card("agents", "Agent 总数", formatCount(agentCount), "—", true),
                        card("knowledge", "知识库", formatCount(kbCount), "—", true),
                        card("invocations", "调用次数", formatCount(totalCalls), changePercent(calls7d, callsPrev7d), calls7d >= callsPrev7d),
                        card("tokens", "Token 消耗", formatCount(totalTokens), changePercent(tokens7d, tokensPrev7d), tokens7d >= tokensPrev7d),
                        card("cost", "成本（元）", formatCost(totalCost), "—", true)
                ))
                .recentItems(buildRecentItems(tenantId, userId))
                .recentLogs(buildRecentLogs(tenantId))
                .modelUsage(buildModelUsage(modelAggregates, modelTokenSum))
                .topApps(buildTopApps(tenantId))
                .systemHealth(List.of(
                        health("API 服务", true),
                        health("向量数据库", true),
                        health("对象存储", true),
                        health("模型服务", true)
                ))
                .trend(buildTrend(tenantId))
                .quickActions(List.of(
                        action("agent", "Agent 管理", "/agent"),
                        action("knowledge", "知识库", "/knowledge"),
                        action("model", "模型配置", "/model"),
                        action("settings", "系统设置", "/settings")
                ))
                .planInfo(DashboardOverviewVO.PlanInfoVO.builder()
                        .planType("标准版")
                        .expireAt("—")
                        .usedPercent(estimateUsagePercent(totalTokens))
                        .build())
                .build();
    }

    private List<DashboardOverviewVO.RecentItemVO> buildRecentItems(Long tenantId, Long userId) {
        if (userId == null) {
            return List.of();
        }
        List<UserRecentAccessEntity> records = recentAccessService.listRecent(tenantId, userId, 5);
        if (records.isEmpty()) {
            return List.of();
        }
        List<DashboardOverviewVO.RecentItemVO> items = new ArrayList<>();
        for (UserRecentAccessEntity record : records) {
            items.add(recent(
                    record.getResourceName(),
                    resourceTypeLabel(record.getResourceType()),
                    formatRelativeTime(record.getAccessedAt()),
                    resourcePath(record.getResourceType(), record.getResourceId())
            ));
        }
        return items;
    }

    private List<DashboardOverviewVO.RecentLogVO> buildRecentLogs(Long tenantId) {
        List<RecentUsageLogRow> rows = dashboardStatsMapper.recentUsageLogs(tenantId, 5);
        if (rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .map(row -> log(
                        row.getAgentName(),
                        true,
                        formatRelativeTime(row.getCreatedAt()),
                        formatDuration(row.getLatencyMs()),
                        row.getTotalTokens() != null ? row.getTotalTokens() : 0
                ))
                .toList();
    }

    private List<DashboardOverviewVO.ModelUsageVO> buildModelUsage(List<ModelUsageAggregate> aggregates, long tokenSum) {
        if (aggregates.isEmpty()) {
            return List.of();
        }
        return aggregates.stream()
                .map(item -> model(
                        displayModelName(item),
                        percent(safeLong(item.getTokens()), tokenSum),
                        formatCount(safeLong(item.getTokens()))
                ))
                .toList();
    }

    private List<DashboardOverviewVO.TopAppVO> buildTopApps(Long tenantId) {
        List<NamedCountRow> rows = dashboardStatsMapper.topAgents(tenantId);
        if (rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .map(row -> top(row.getName(), formatCount(safeLong(row.getValue()))))
                .toList();
    }

    private List<DashboardOverviewVO.TrendPointVO> buildTrend(Long tenantId) {
        List<TrendPointRow> rows = dashboardStatsMapper.hourlyTrend(tenantId);
        if (rows.isEmpty()) {
            return List.of(
                    trend("00:00", 0), trend("04:00", 0), trend("08:00", 0),
                    trend("12:00", 0), trend("16:00", 0), trend("20:00", 0)
            );
        }
        return rows.stream()
                .map(row -> trend(row.getTimeLabel(), safeLong(row.getValue())))
                .toList();
    }

    private String resourceTypeLabel(String resourceType) {
        if ("knowledge".equals(resourceType)) {
            return "知识库";
        }
        if ("agent".equals(resourceType)) {
            return "Agent";
        }
        return resourceType;
    }

    private String resourcePath(String resourceType, Long resourceId) {
        if ("knowledge".equals(resourceType)) {
            return "/knowledge/" + resourceId;
        }
        if ("agent".equals(resourceType)) {
            return "/agent";
        }
        return "/";
    }

    private String displayModelName(ModelUsageAggregate item) {
        if (item.getDisplayName() != null && !item.getDisplayName().isBlank()) {
            return item.getDisplayName();
        }
        return item.getModelName() != null ? item.getModelName() : "未知模型";
    }

    private int percent(long part, long total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round(part * 100.0 / total);
    }

    private int estimateUsagePercent(long totalTokens) {
        if (totalTokens <= 0) {
            return 0;
        }
        return (int) Math.min(100, totalTokens / 100_000);
    }

    private String changePercent(long current, long previous) {
        if (previous <= 0) {
            return current > 0 ? "+100%" : "—";
        }
        long delta = Math.round((current - previous) * 100.0 / previous);
        return (delta >= 0 ? "+" : "") + delta + "%";
    }

    private String formatCount(long value) {
        if (value >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0);
        }
        if (value >= 1_000) {
            return String.format(Locale.ROOT, "%.1fK", value / 1_000.0);
        }
        return String.valueOf(value);
    }

    private String formatCost(BigDecimal cost) {
        BigDecimal safe = cost != null ? cost : BigDecimal.ZERO;
        return "¥" + safe.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatDuration(Integer latencyMs) {
        if (latencyMs == null || latencyMs <= 0) {
            return "-";
        }
        if (latencyMs < 1000) {
            return latencyMs + "ms";
        }
        return String.format(Locale.ROOT, "%.1fs", latencyMs / 1000.0);
    }

    private String formatRelativeTime(LocalDateTime time) {
        if (time == null) {
            return "—";
        }
        long minutes = ChronoUnit.MINUTES.between(time, LocalDateTime.now());
        if (minutes < 1) {
            return "刚刚";
        }
        if (minutes < 60) {
            return minutes + " 分钟前";
        }
        long hours = Duration.ofMinutes(minutes).toHours();
        if (hours < 24) {
            return hours + " 小时前";
        }
        long days = hours / 24;
        return days + " 天前";
    }

    private long safeLong(Long value) {
        return value != null ? value : 0L;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户上下文缺失");
        }
        return tenantId;
    }

    private DashboardOverviewVO.StatCardVO card(String key, String label, String value, String change, boolean up) {
        return DashboardOverviewVO.StatCardVO.builder().key(key).label(label).value(value).change(change).up(up).build();
    }

    private DashboardOverviewVO.RecentItemVO recent(String name, String type, String updatedAt, String path) {
        return DashboardOverviewVO.RecentItemVO.builder().name(name).type(type).updatedAt(updatedAt).path(path).build();
    }

    private DashboardOverviewVO.RecentLogVO log(String name, boolean success, String time, String duration, int tokens) {
        return DashboardOverviewVO.RecentLogVO.builder()
                .name(name).success(success).status(success ? "成功" : "失败")
                .time(time).duration(duration).tokens(tokens).build();
    }

    private DashboardOverviewVO.ModelUsageVO model(String model, int percent, String tokens) {
        return DashboardOverviewVO.ModelUsageVO.builder().model(model).percent(percent).tokens(tokens).build();
    }

    private DashboardOverviewVO.TopAppVO top(String name, String value) {
        return DashboardOverviewVO.TopAppVO.builder().name(name).value(value).build();
    }

    private DashboardOverviewVO.SystemHealthVO health(String name, boolean healthy) {
        return DashboardOverviewVO.SystemHealthVO.builder()
                .name(name).healthy(healthy).status(healthy ? "正常" : "异常").build();
    }

    private DashboardOverviewVO.TrendPointVO trend(String time, long value) {
        return DashboardOverviewVO.TrendPointVO.builder().time(time).value(value).build();
    }

    private DashboardOverviewVO.QuickActionVO action(String key, String label, String path) {
        return DashboardOverviewVO.QuickActionVO.builder().key(key).label(label).path(path).build();
    }
}
