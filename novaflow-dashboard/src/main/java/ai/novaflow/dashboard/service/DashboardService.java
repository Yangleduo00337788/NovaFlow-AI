package ai.novaflow.dashboard.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.dashboard.domain.DailySparklineRow;
import ai.novaflow.dashboard.domain.DashboardOverviewVO;
import ai.novaflow.dashboard.domain.NamedCountRow;
import ai.novaflow.dashboard.domain.PublishedWorkflowRow;
import ai.novaflow.dashboard.domain.RecentUsageLogRow;
import ai.novaflow.dashboard.domain.TrendPointRow;
import ai.novaflow.dashboard.domain.WorkflowNodeLogRow;
import ai.novaflow.dashboard.domain.WorkflowEdgeRow;
import ai.novaflow.dashboard.domain.WorkflowNodeRow;
import ai.novaflow.dashboard.domain.WorkflowRuntimeRow;
import ai.novaflow.dashboard.domain.dto.FavoriteToggleRequest;
import ai.novaflow.dashboard.mapper.DashboardStatsMapper;
import ai.novaflow.model.domain.BillingCurrency;
import ai.novaflow.model.domain.ModelUsageAggregate;
import ai.novaflow.model.mapper.TokenUsageMapper;
import ai.novaflow.monitor.domain.vo.MonitorOverviewVO;
import ai.novaflow.monitor.service.InfrastructureHealthChecker;
import ai.novaflow.user.domain.vo.TenantPlanSummaryVO;
import ai.novaflow.user.entity.UserFavoriteEntity;
import ai.novaflow.user.entity.UserRecentAccessEntity;
import ai.novaflow.user.service.FavoriteService;
import ai.novaflow.user.service.OrganizationService;
import ai.novaflow.user.service.RecentAccessService;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Set<String> DASHBOARD_HEALTH_KEYS = Set.of("api", "qdrant", "redis", "minio");
    private static final Map<String, String> HEALTH_DISPLAY_NAMES = Map.of(
            "api", "API 服务",
            "qdrant", "向量数据库",
            "redis", "消息队列",
            "minio", "存储服务"
    );

    private final DashboardStatsMapper dashboardStatsMapper;
    private final TokenUsageMapper tokenUsageMapper;
    private final RecentAccessService recentAccessService;
    private final FavoriteService favoriteService;
    private final OrganizationService organizationService;
    private final InfrastructureHealthChecker infrastructureHealthChecker;
    private final ObjectMapper objectMapper;

    public DashboardOverviewVO getOverview() {
        Long tenantId = requireTenantId();
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;

        long appCount = safeLong(dashboardStatsMapper.countApplications(tenantId));
        long agentCount = safeLong(dashboardStatsMapper.countAgents(tenantId));
        long kbCount = safeLong(dashboardStatsMapper.countKnowledgeBases(tenantId));
        long appsPrev7d = safeLong(dashboardStatsMapper.countApplicationsBeforeDays(tenantId, 7));
        long agentsPrev7d = safeLong(dashboardStatsMapper.countAgentsBeforeDays(tenantId, 7));
        long kbPrev7d = safeLong(dashboardStatsMapper.countKnowledgeBasesBeforeDays(tenantId, 7));

        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();
        YearMonth previousMonth = currentMonth.minusMonths(1);
        LocalDate prevMonthStart = previousMonth.atDay(1);
        LocalDate prevMonthEnd = previousMonth.atEndOfMonth();

        long monthlyCalls = safeLong(tokenUsageMapper.countCallsBetween(tenantId, monthStart, monthEnd));
        long monthlyTokens = safeLong(tokenUsageMapper.sumTokensBetween(tenantId, monthStart, monthEnd));
        long prevMonthlyCalls = safeLong(tokenUsageMapper.countCallsBetween(tenantId, prevMonthStart, prevMonthEnd));
        long prevMonthlyTokens = safeLong(tokenUsageMapper.sumTokensBetween(tenantId, prevMonthStart, prevMonthEnd));

        BigDecimal monthlyCnyCost = safeDecimal(tokenUsageMapper.sumCostBetween(
                tenantId, BillingCurrency.CNY.getCode(), monthStart, monthEnd));
        BigDecimal monthlyUsdCost = safeDecimal(tokenUsageMapper.sumCostBetween(
                tenantId, BillingCurrency.USD.getCode(), monthStart, monthEnd));
        BigDecimal prevMonthlyCnyCost = safeDecimal(tokenUsageMapper.sumCostBetween(
                tenantId, BillingCurrency.CNY.getCode(), prevMonthStart, prevMonthEnd));
        BigDecimal prevMonthlyUsdCost = safeDecimal(tokenUsageMapper.sumCostBetween(
                tenantId, BillingCurrency.USD.getCode(), prevMonthStart, prevMonthEnd));
        long monthlyCostMinor = toCostMinorUnits(monthlyCnyCost, monthlyUsdCost);
        long prevMonthlyCostMinor = toCostMinorUnits(prevMonthlyCnyCost, prevMonthlyUsdCost);

        List<ModelUsageAggregate> modelAggregates = tokenUsageMapper.topModelsByTenant(tenantId);
        long modelTokenSum = modelAggregates.stream().mapToLong(item -> safeLong(item.getTokens())).sum();

        TenantPlanSummaryVO planSummary = organizationService.getPlanSummary();
        Set<String> favoriteKeys = userId != null ? favoriteService.favoriteKeys(userId) : Set.of();

        return DashboardOverviewVO.builder()
                .stats(List.of(
                        card("apps", "应用总数", formatCount(appCount),
                                changePercent(appCount, appsPrev7d), appCount >= appsPrev7d),
                        card("agents", "Agent 总数", formatCount(agentCount),
                                changePercent(agentCount, agentsPrev7d), agentCount >= agentsPrev7d),
                        card("knowledge", "知识库", formatCount(kbCount),
                                changePercent(kbCount, kbPrev7d), kbCount >= kbPrev7d),
                        card("invocations", "本月调用", formatCount(monthlyCalls),
                                changePercent(monthlyCalls, prevMonthlyCalls), monthlyCalls >= prevMonthlyCalls),
                        card("tokens", "本月 Token", formatCount(monthlyTokens),
                                changePercent(monthlyTokens, prevMonthlyTokens), monthlyTokens >= prevMonthlyTokens),
                        card("cost", "本月费用", formatCombinedCost(monthlyCnyCost, monthlyUsdCost),
                                changePercent(monthlyCostMinor, prevMonthlyCostMinor), monthlyCostMinor <= prevMonthlyCostMinor)
                ))
                .recentItems(buildRecentItems(tenantId, userId, favoriteKeys))
                .favoriteItems(buildFavoriteItems(tenantId, userId))
                .recentLogs(buildRecentLogs(tenantId))
                .modelUsage(buildModelUsage(modelAggregates, modelTokenSum))
                .topApps(buildTopApps(tenantId))
                .workflowRuntime(buildWorkflowRuntime(tenantId))
                .systemHealth(buildSystemHealth())
                .trend(buildTrend(tenantId))
                .quickActions(List.of(
                        action("api-key", "API Key 管理", "/settings"),
                        action("prompt", "Prompt 模板", "/prompt"),
                        action("dataset", "数据集管理", "/knowledge"),
                        action("mcp", "MCP 服务", "/tool"),
                        action("settings", "系统设置", "/settings"),
                        action("users", "用户管理", "/org")
                ))
                .quickStartTiles(buildQuickStartTiles())
                .planInfo(DashboardOverviewVO.PlanInfoVO.builder()
                        .planType(planSummary.getPlanTypeLabel())
                        .expireAt(formatExpireAt(planSummary.getExpireAt()))
                        .usedPercent(planSummary.getTokenUsedPercent() != null
                                ? planSummary.getTokenUsedPercent()
                                : planSummary.getUsedPercent())
                        .build())
                .totalModelTokens(formatCount(modelTokenSum))
                .sparklines(buildSparklines(tenantId, appCount, agentCount, kbCount, monthlyCnyCost, monthlyUsdCost))
                .build();
    }

    public boolean toggleFavorite(FavoriteToggleRequest request) {
        if (request == null || request.getResourceId() == null || request.getResourceType() == null) {
            throw new BusinessException("收藏参数不完整");
        }
        if (!StpUtil.isLogin()) {
            throw new BusinessException("请先登录");
        }
        Long tenantId = requireTenantId();
        Long userId = StpUtil.getLoginIdAsLong();
        String resourceName = request.getResourceName() != null ? request.getResourceName().trim() : "未命名资源";
        if (resourceName.isEmpty()) {
            resourceName = "未命名资源";
        }
        return favoriteService.toggle(
                tenantId,
                userId,
                request.getResourceType().trim(),
                request.getResourceId(),
                resourceName
        );
    }

    public List<DashboardOverviewVO.RecentItemVO> listRecentItems(int limit) {
        Long tenantId = requireTenantId();
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        if (userId == null) {
            return List.of();
        }
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        Set<String> favoriteKeys = favoriteService.favoriteKeys(userId);
        return buildRecentItems(tenantId, userId, favoriteKeys, safeLimit);
    }

    public List<DashboardOverviewVO.RecentItemVO> listFavoriteItems(int limit) {
        Long tenantId = requireTenantId();
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        if (userId == null) {
            return List.of();
        }
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return buildFavoriteItems(tenantId, userId, safeLimit);
    }

    public List<DashboardOverviewVO.PublishedWorkflowVO> listPublishedWorkflows(int limit) {
        Long tenantId = requireTenantId();
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        List<PublishedWorkflowRow> rows = dashboardStatsMapper.listPublishedWorkflows(tenantId, safeLimit);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .map(row -> {
                    int status = row.getStatus() != null ? row.getStatus() : 1;
                    return DashboardOverviewVO.PublishedWorkflowVO.builder()
                            .workflowId(row.getWorkflowId())
                            .workflowName(row.getWorkflowName())
                            .applicationName(row.getApplicationName())
                            .status(status)
                            .statusLabel(workflowStatusLabel(status))
                            .path("/workflow/" + row.getWorkflowId())
                            .updatedAt(formatRelativeTime(row.getUpdatedAt()))
                            .build();
                })
                .toList();
    }

    private List<DashboardOverviewVO.QuickStartTileVO> buildQuickStartTiles() {
        return List.of(
                quickStart("agent", "创建 Agent", "快速创建智能助手", "/agent?create=1", "blue"),
                quickStart("workflow", "创建工作流", "可视化编排流程", "/workflow?create=1", "teal"),
                quickStart("knowledge", "创建知识库", "构建企业知识中心", "/knowledge?create=1", "orange"),
                quickStart("import", "导入文档", "支持多种格式", "/knowledge?import=1", "purple")
        );
    }

    private Map<String, List<Long>> buildSparklines(
            Long tenantId,
            long appCount,
            long agentCount,
            long kbCount,
            BigDecimal monthlyCnyCost,
            BigDecimal monthlyUsdCost) {
        List<Long> invocationSeries = toSparklineValues(dashboardStatsMapper.dailyInvocationSparkline(tenantId));
        List<Long> tokenSeries = toSparklineValues(dashboardStatsMapper.dailyTokenSparkline(tenantId));
        List<Long> costSeries = toCostSparklineValues(
                dashboardStatsMapper.dailyCostSparkline(tenantId, BillingCurrency.CNY.getCode()),
                dashboardStatsMapper.dailyCostSparkline(tenantId, BillingCurrency.USD.getCode()));
        Map<String, List<Long>> sparklines = new LinkedHashMap<>();
        sparklines.put("apps", buildCumulativeSparkline(
                dashboardStatsMapper.dailyApplicationSparkline(tenantId), appCount));
        sparklines.put("agents", buildCumulativeSparkline(
                dashboardStatsMapper.dailyAgentSparkline(tenantId), agentCount));
        sparklines.put("knowledge", buildCumulativeSparkline(
                dashboardStatsMapper.dailyKnowledgeSparkline(tenantId), kbCount));
        sparklines.put("invocations", invocationSeries);
        sparklines.put("tokens", tokenSeries);
        sparklines.put("cost", costSeries.isEmpty()
                ? repeatValue(toCostMinorUnits(monthlyCnyCost, monthlyUsdCost), 7)
                : costSeries);
        return sparklines;
    }

    private List<Long> buildCumulativeSparkline(List<DailySparklineRow> dailyNewRows, long currentTotal) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        Map<LocalDate, Long> dailyMap = new HashMap<>();
        long newInWindow = 0;
        if (dailyNewRows != null) {
            for (DailySparklineRow row : dailyNewRows) {
                if (row.getStatDate() != null) {
                    dailyMap.put(row.getStatDate(), safeLong(row.getValue()));
                    newInWindow += safeLong(row.getValue());
                }
            }
        }
        long base = Math.max(0, currentTotal - newInWindow);
        List<Long> series = new ArrayList<>(7);
        long running = base;
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            running += dailyMap.getOrDefault(day, 0L);
            series.add(running);
        }
        if (!series.isEmpty()) {
            series.set(series.size() - 1, currentTotal);
        }
        return series.isEmpty() ? repeatValue(currentTotal, 7) : series;
    }

    private List<Long> toCostSparklineValues(List<DailySparklineRow> cnyRows, List<DailySparklineRow> usdRows) {
        if ((cnyRows == null || cnyRows.isEmpty()) && (usdRows == null || usdRows.isEmpty())) {
            return List.of();
        }
        int size = Math.max(cnyRows != null ? cnyRows.size() : 0, usdRows != null ? usdRows.size() : 0);
        List<Long> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            long cny = i < (cnyRows != null ? cnyRows.size() : 0)
                    ? safeLong(cnyRows.get(i).getValue())
                    : 0L;
            long usd = i < (usdRows != null ? usdRows.size() : 0)
                    ? safeLong(usdRows.get(i).getValue())
                    : 0L;
            values.add(cny + usd);
        }
        return values;
    }

    private List<Long> toSparklineValues(List<DailySparklineRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return repeatValue(0L, 7);
        }
        return rows.stream().map(row -> safeLong(row.getValue())).toList();
    }

    private List<Long> repeatValue(long value, int size) {
        List<Long> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(value);
        }
        return list;
    }

    private List<DashboardOverviewVO.RecentItemVO> buildRecentItems(Long tenantId, Long userId, Set<String> favoriteKeys) {
        return buildRecentItems(tenantId, userId, favoriteKeys, 10);
    }

    private List<DashboardOverviewVO.RecentItemVO> buildRecentItems(
            Long tenantId,
            Long userId,
            Set<String> favoriteKeys,
            int limit) {
        if (userId == null) {
            return List.of();
        }
        List<UserRecentAccessEntity> records = recentAccessService.listRecent(tenantId, userId, limit);
        if (records.isEmpty()) {
            return List.of();
        }
        List<DashboardOverviewVO.RecentItemVO> items = new ArrayList<>();
        for (UserRecentAccessEntity record : records) {
            items.add(toRecentItem(record, favoriteKeys.contains(
                    favoriteService.key(record.getResourceType(), record.getResourceId())),
                    formatRelativeTime(record.getAccessedAt())));
        }
        return items;
    }

    private List<DashboardOverviewVO.RecentItemVO> buildFavoriteItems(Long tenantId, Long userId) {
        return buildFavoriteItems(tenantId, userId, 10);
    }

    private List<DashboardOverviewVO.RecentItemVO> buildFavoriteItems(Long tenantId, Long userId, int limit) {
        if (userId == null) {
            return List.of();
        }
        List<UserFavoriteEntity> records = favoriteService.listFavorites(tenantId, userId, limit);
        if (records.isEmpty()) {
            return List.of();
        }
        List<DashboardOverviewVO.RecentItemVO> items = new ArrayList<>();
        for (UserFavoriteEntity record : records) {
            items.add(toRecentItem(record.getResourceType(), record.getResourceId(), record.getResourceName(),
                    true, formatRelativeTime(record.getCreatedAt())));
        }
        return items;
    }

    private DashboardOverviewVO.RecentItemVO toRecentItem(
            UserRecentAccessEntity record,
            boolean favorite,
            String updatedAt) {
        return toRecentItem(record.getResourceType(), record.getResourceId(), record.getResourceName(),
                favorite, updatedAt);
    }

    private DashboardOverviewVO.RecentItemVO toRecentItem(
            String resourceType,
            Long resourceId,
            String resourceName,
            boolean favorite,
            String updatedAt) {
        return DashboardOverviewVO.RecentItemVO.builder()
                .name(resourceName)
                .type(resourceTypeLabel(resourceType))
                .updatedAt(updatedAt)
                .path(resourcePath(resourceType, resourceId))
                .resourceType(resourceType)
                .resourceId(resourceId)
                .favorite(favorite)
                .build();
    }

    private List<DashboardOverviewVO.RecentLogVO> buildRecentLogs(Long tenantId) {
        List<RecentUsageLogRow> rows = dashboardStatsMapper.recentUsageLogs(tenantId, 5);
        if (rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .map(row -> {
                    boolean success = row.getSuccess() == null || row.getSuccess() != 0;
                    return log(
                            row.getId(),
                            row.getTraceId(),
                            row.getAgentName(),
                            success,
                            formatRelativeTime(row.getCreatedAt()),
                            formatDuration(row.getLatencyMs()),
                            row.getTotalTokens() != null ? row.getTotalTokens() : 0
                    );
                })
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
        List<NamedCountRow> rows = dashboardStatsMapper.topApplications(tenantId);
        if (rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .map(row -> top(row.getName(), formatCount(safeLong(row.getValue()))))
                .toList();
    }

    public DashboardOverviewVO.WorkflowRuntimeVO getWorkflowRuntime(Long workflowId) {
        if (workflowId == null) {
            throw new BusinessException("工作流 ID 不能为空");
        }
        Long tenantId = requireTenantId();
        WorkflowRuntimeRow workflow = dashboardStatsMapper.findWorkflow(tenantId, workflowId);
        if (workflow == null) {
            throw new BusinessException("工作流不存在");
        }
        return buildWorkflowRuntimeForWorkflow(tenantId, workflowId);
    }

    private DashboardOverviewVO.WorkflowRuntimeVO buildWorkflowRuntime(Long tenantId) {
        WorkflowRuntimeRow workflow = dashboardStatsMapper.latestPublishedWorkflow(tenantId);
        if (workflow == null || workflow.getWorkflowId() == null) {
            return null;
        }
        return buildWorkflowRuntimeForWorkflow(tenantId, workflow.getWorkflowId());
    }

    private DashboardOverviewVO.WorkflowRuntimeVO buildWorkflowRuntimeForWorkflow(Long tenantId, Long workflowId) {
        WorkflowRuntimeRow workflow = dashboardStatsMapper.findWorkflow(tenantId, workflowId);
        if (workflow == null) {
            return null;
        }
        List<WorkflowNodeRow> nodes = dashboardStatsMapper.workflowNodes(workflowId);
        WorkflowRuntimeRow execution = dashboardStatsMapper.latestExecutionForWorkflow(tenantId, workflowId);

        Map<String, Integer> nodeStatusMap = new HashMap<>();
        List<DashboardOverviewVO.WorkflowRuntimeNodeVO> runtimeNodes = new ArrayList<>();
        boolean running = false;
        int displayStatus = workflow.getStatus() != null ? workflow.getStatus() : 1;
        String statusLabel = workflowStatusLabel(displayStatus);
        String executionId = null;

        if (execution != null && StringUtils.hasText(execution.getExecutionId())) {
            executionId = execution.getExecutionId();
            int execStatus = execution.getStatus() != null ? execution.getStatus() : 1;
            running = execStatus == 0;
            displayStatus = execStatus;
            statusLabel = executionStatusLabel(execStatus);
            for (WorkflowNodeLogRow log : dashboardStatsMapper.workflowNodeLogs(executionId)) {
                int nodeStatus = log.getStatus() != null ? log.getStatus() : -1;
                nodeStatusMap.put(log.getNodeId(), nodeStatus);
                WorkflowNodeRow dbNode = nodes.stream()
                        .filter(item -> log.getNodeId().equals(item.getNodeId()))
                        .findFirst()
                        .orElse(null);
                runtimeNodes.add(DashboardOverviewVO.WorkflowRuntimeNodeVO.builder()
                        .nodeId(log.getNodeId())
                        .nodeName(dbNode != null ? dbNode.getNodeName() : log.getNodeId())
                        .nodeType(dbNode != null ? dbNode.getNodeType() : "llm")
                        .status(resolveNodeStatus(nodeStatus, running))
                        .statusLabel(nodeStatusLabel(resolveNodeStatus(nodeStatus, running)))
                        .build());
            }
        }

        return DashboardOverviewVO.WorkflowRuntimeVO.builder()
                .workflowId(workflowId)
                .workflowName(workflow.getWorkflowName())
                .executionId(executionId)
                .status(displayStatus)
                .statusLabel(statusLabel)
                .running(running)
                .path("/workflow/" + workflowId)
                .nodes(runtimeNodes)
                .canvas(buildWorkflowCanvas(workflowId, nodes, nodeStatusMap, running))
                .build();
    }

    private DashboardOverviewVO.WorkflowCanvasVO buildWorkflowCanvas(
            Long workflowId,
            List<WorkflowNodeRow> dbNodes,
            Map<String, Integer> nodeStatusMap,
            boolean executionRunning) {
        Map<String, WorkflowNodeRow> dbNodeMap = new HashMap<>();
        for (WorkflowNodeRow node : dbNodes) {
            dbNodeMap.put(node.getNodeId(), node);
        }
        String canvasJson = dashboardStatsMapper.workflowCanvasData(workflowId);
        if (StringUtils.hasText(canvasJson)) {
            try {
                Map<String, Object> payload = objectMapper.readValue(canvasJson, new TypeReference<>() {});
                return buildCanvasFromPayload(payload, dbNodeMap, nodeStatusMap, executionRunning, workflowId);
            } catch (Exception ignored) {
                // fallback to db nodes
            }
        }
        return buildCanvasFromDbNodes(workflowId, dbNodes, nodeStatusMap, executionRunning);
    }

    @SuppressWarnings("unchecked")
    private DashboardOverviewVO.WorkflowCanvasVO buildCanvasFromPayload(
            Map<String, Object> payload,
            Map<String, WorkflowNodeRow> dbNodeMap,
            Map<String, Integer> nodeStatusMap,
            boolean executionRunning,
            Long workflowId) {
        List<Map<String, Object>> rawNodes = (List<Map<String, Object>>) payload.get("nodes");
        List<Map<String, Object>> rawEdges = (List<Map<String, Object>>) payload.get("edges");
        List<DashboardOverviewVO.WorkflowCanvasNodeVO> canvasNodes = new ArrayList<>();
        if (rawNodes != null) {
            for (Map<String, Object> rawNode : rawNodes) {
                String nodeId = stringValue(rawNode.get("id"));
                if (!StringUtils.hasText(nodeId)) {
                    continue;
                }
                WorkflowNodeRow dbNode = dbNodeMap.get(nodeId);
                String type = stringValue(rawNode.get("type"));
                if (!StringUtils.hasText(type) && dbNode != null) {
                    type = dbNode.getNodeType();
                }
                Map<String, Object> data = rawNode.get("data") instanceof Map<?, ?> dataMap
                        ? (Map<String, Object>) dataMap
                        : Map.of();
                String label = stringValue(data.get("label"));
                if (!StringUtils.hasText(label) && dbNode != null) {
                    label = dbNode.getNodeName();
                }
                Map<String, Object> position = rawNode.get("position") instanceof Map<?, ?> posMap
                        ? (Map<String, Object>) posMap
                        : Map.of();
                double x = toDouble(position.get("x"), dbNode != null ? dbNode.getPositionX() : null, 0);
                double y = toDouble(position.get("y"), dbNode != null ? dbNode.getPositionY() : null, 0);
                int status = resolveNodeStatus(nodeStatusMap.get(nodeId), executionRunning);
                canvasNodes.add(canvasNode(nodeId, type, x, y, label, status));
            }
        }
        List<DashboardOverviewVO.WorkflowCanvasEdgeVO> canvasEdges = new ArrayList<>();
        if (rawEdges != null) {
            for (Map<String, Object> rawEdge : rawEdges) {
                String edgeId = stringValue(rawEdge.get("id"));
                String source = stringValue(rawEdge.get("source"));
                String target = stringValue(rawEdge.get("target"));
                if (!StringUtils.hasText(source) || !StringUtils.hasText(target)) {
                    continue;
                }
                canvasEdges.add(DashboardOverviewVO.WorkflowCanvasEdgeVO.builder()
                        .id(StringUtils.hasText(edgeId) ? edgeId : source + "-" + target)
                        .source(source)
                        .target(target)
                        .sourceHandle(stringValue(rawEdge.get("sourceHandle")))
                        .targetHandle(stringValue(rawEdge.get("targetHandle")))
                        .label(stringValue(rawEdge.get("label")))
                        .build());
            }
        }
        if (canvasEdges.isEmpty()) {
            canvasEdges = buildCanvasEdges(workflowId);
        }
        if (canvasNodes.isEmpty()) {
            return buildCanvasFromDbNodes(workflowId, new ArrayList<>(dbNodeMap.values()), nodeStatusMap, executionRunning);
        }
        return DashboardOverviewVO.WorkflowCanvasVO.builder()
                .nodes(canvasNodes)
                .edges(canvasEdges)
                .build();
    }

    private DashboardOverviewVO.WorkflowCanvasVO buildCanvasFromDbNodes(
            Long workflowId,
            List<WorkflowNodeRow> dbNodes,
            Map<String, Integer> nodeStatusMap,
            boolean executionRunning) {
        List<DashboardOverviewVO.WorkflowCanvasNodeVO> canvasNodes = new ArrayList<>();
        int index = 0;
        for (WorkflowNodeRow node : dbNodes) {
            double x = node.getPositionX() != null ? node.getPositionX().doubleValue() : 120 + index * 180.0;
            double y = node.getPositionY() != null ? node.getPositionY().doubleValue() : 120;
            int status = resolveNodeStatus(nodeStatusMap.get(node.getNodeId()), executionRunning);
            canvasNodes.add(canvasNode(node.getNodeId(), node.getNodeType(), x, y, node.getNodeName(), status));
            index++;
        }
        return DashboardOverviewVO.WorkflowCanvasVO.builder()
                .nodes(canvasNodes)
                .edges(buildCanvasEdges(workflowId))
                .build();
    }

    private List<DashboardOverviewVO.WorkflowCanvasEdgeVO> buildCanvasEdges(Long workflowId) {
        List<WorkflowEdgeRow> edges = dashboardStatsMapper.workflowEdges(workflowId);
        if (edges == null || edges.isEmpty()) {
            return List.of();
        }
        List<DashboardOverviewVO.WorkflowCanvasEdgeVO> canvasEdges = new ArrayList<>();
        for (WorkflowEdgeRow edge : edges) {
            canvasEdges.add(DashboardOverviewVO.WorkflowCanvasEdgeVO.builder()
                    .id(edge.getEdgeId())
                    .source(edge.getSourceNodeId())
                    .target(edge.getTargetNodeId())
                    .sourceHandle(edge.getSourceHandle())
                    .targetHandle(edge.getTargetHandle())
                    .label(edge.getConditionExpr())
                    .build());
        }
        return canvasEdges;
    }

    private DashboardOverviewVO.WorkflowCanvasNodeVO canvasNode(
            String id,
            String type,
            double x,
            double y,
            String label,
            int status) {
        return DashboardOverviewVO.WorkflowCanvasNodeVO.builder()
                .id(id)
                .type(StringUtils.hasText(type) ? type : "llm")
                .position(DashboardOverviewVO.CanvasPositionVO.builder().x(x).y(y).build())
                .data(DashboardOverviewVO.CanvasNodeDataVO.builder()
                        .label(StringUtils.hasText(label) ? label : id)
                        .build())
                .status(status)
                .statusLabel(status >= 0 ? nodeStatusLabel(status) : null)
                .build();
    }

    private String stringValue(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private double toDouble(Object value, java.math.BigDecimal fallback, double defaultValue) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (fallback != null) {
            return fallback.doubleValue();
        }
        return defaultValue;
    }

    private int resolveNodeStatus(Integer logStatus, boolean executionRunning) {
        if (logStatus == null) {
            return executionRunning ? -1 : -1;
        }
        if (logStatus == 0) {
            return 0;
        }
        if (logStatus == 2) {
            return 2;
        }
        return 1;
    }

    private String nodeStatusLabel(int status) {
        return switch (status) {
            case 0 -> "运行中";
            case 1 -> "完成";
            case 2 -> "失败";
            default -> "待执行";
        };
    }

    private String workflowStatusLabel(int status) {
        return status == 1 ? "已发布" : "草稿";
    }

    private String executionStatusLabel(int status) {
        return switch (status) {
            case 0 -> "运行中";
            case 1 -> "已完成";
            case 2 -> "失败";
            case 3 -> "超时";
            default -> "未知";
        };
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

    private List<DashboardOverviewVO.SystemHealthVO> buildSystemHealth() {
        return infrastructureHealthChecker.checkAll().stream()
                .filter(service -> DASHBOARD_HEALTH_KEYS.contains(service.getKey()))
                .map(this::toDashboardHealth)
                .toList();
    }

    private DashboardOverviewVO.SystemHealthVO toDashboardHealth(MonitorOverviewVO.ServiceHealthVO service) {
        String displayName = HEALTH_DISPLAY_NAMES.getOrDefault(service.getKey(), service.getName());
        return health(displayName, service.isHealthy());
    }

    private String resourceTypeLabel(String resourceType) {
        if ("knowledge".equals(resourceType)) {
            return "知识库";
        }
        if ("agent".equals(resourceType)) {
            return "Agent";
        }
        if ("workflow".equals(resourceType)) {
            return "工作流";
        }
        return resourceType;
    }

    private String resourcePath(String resourceType, Long resourceId) {
        if ("knowledge".equals(resourceType)) {
            return "/knowledge/" + resourceId;
        }
        if ("agent".equals(resourceType)) {
            return "/agent?id=" + resourceId;
        }
        if ("workflow".equals(resourceType)) {
            return "/workflow/" + resourceId;
        }
        return "/";
    }

    private String displayModelName(ModelUsageAggregate item) {
        if (item.getDisplayName() != null && !item.getDisplayName().isBlank()) {
            return item.getDisplayName();
        }
        return item.getModelName() != null ? item.getModelName() : "未知模型";
    }

    private String formatExpireAt(LocalDateTime expireAt) {
        if (expireAt == null) {
            return "—";
        }
        return expireAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
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

    private String formatAmount(BigDecimal cost) {
        if (cost.compareTo(new BigDecimal("0.01")) < 0) {
            return cost.setScale(4, RoundingMode.HALF_UP).toPlainString();
        }
        return cost.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private long toCostMinorUnits(BigDecimal cnyCost, BigDecimal usdCost) {
        BigDecimal cnyMinor = cnyCost.multiply(BigDecimal.valueOf(100));
        BigDecimal usdMinor = usdCost.multiply(BigDecimal.valueOf(100));
        return cnyMinor.add(usdMinor).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private int percent(long part, long total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round(part * 100.0 / total);
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

    private BigDecimal safeDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
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

    private DashboardOverviewVO.RecentLogVO log(
            Long logId,
            String traceId,
            String name,
            boolean success,
            String time,
            String duration,
            int tokens) {
        return DashboardOverviewVO.RecentLogVO.builder()
                .logId(logId)
                .traceId(traceId)
                .name(name)
                .success(success)
                .status(success ? "成功" : "失败")
                .time(time)
                .duration(duration)
                .tokens(tokens)
                .build();
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

    private DashboardOverviewVO.QuickStartTileVO quickStart(
            String key,
            String label,
            String desc,
            String path,
            String color) {
        return DashboardOverviewVO.QuickStartTileVO.builder()
                .key(key)
                .label(label)
                .desc(desc)
                .path(path)
                .color(color)
                .build();
    }
}
