package ai.novaflow.monitor.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.monitor.domain.TraceNodeRow;
import ai.novaflow.monitor.domain.TraceSpanRow;
import ai.novaflow.monitor.domain.TrendPointRow;
import ai.novaflow.monitor.domain.vo.MonitorOverviewVO;
import ai.novaflow.monitor.domain.vo.ObservabilityOverviewVO;
import ai.novaflow.monitor.domain.vo.TraceDetailVO;
import ai.novaflow.monitor.domain.vo.TraceNodeVO;
import ai.novaflow.monitor.domain.vo.TraceSpanVO;
import ai.novaflow.monitor.mapper.MonitorStatsMapper;
import ai.novaflow.monitor.mapper.TraceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TraceService {

    private final TraceMapper traceMapper;
    private final MonitorStatsMapper monitorStatsMapper;
    private final InfrastructureHealthChecker infrastructureHealthChecker;

    public PageResult<TraceSpanVO> pageSpans(
            int page,
            int pageSize,
            String keyword,
            String spanType,
            Integer status,
            String timeRange) {
        Long tenantId = requireTenantId();
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (safePage - 1) * safePageSize;
        LocalDateTime since = resolveSince(timeRange);
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String trimmedType = StringUtils.hasText(spanType) ? spanType.trim() : null;

        Long total = traceMapper.countSpans(tenantId, since, trimmedType, status, trimmedKeyword);
        List<TraceSpanVO> list = traceMapper.pageSpans(
                        tenantId, since, trimmedType, status, trimmedKeyword, offset, safePageSize)
                .stream()
                .map(this::toSpanVO)
                .toList();
        return PageResult.of(list, total != null ? total : 0L, safePage, safePageSize);
    }

    public TraceDetailVO getSpanDetail(String traceId) {
        if (!StringUtils.hasText(traceId)) {
            throw new BusinessException("Trace ID 不能为空");
        }
        Long tenantId = requireTenantId();
        String trimmedTraceId = traceId.trim();

        TraceSpanRow workflowSpan = traceMapper.findWorkflowSpan(tenantId, trimmedTraceId);
        if (workflowSpan != null) {
            List<TraceNodeVO> nodes = traceMapper.workflowNodes(tenantId, trimmedTraceId).stream()
                    .map(this::toNodeVO)
                    .toList();
            return toDetailVO(workflowSpan, nodes);
        }

        TraceSpanRow agentSpan = traceMapper.findAgentSpan(tenantId, trimmedTraceId);
        if (agentSpan == null) {
            throw new BusinessException("未找到对应链路");
        }
        TraceNodeVO chatNode = TraceNodeVO.builder()
                .nodeId("chat")
                .nodeName("Agent 对话")
                .nodeType("agent")
                .status(agentSpan.getStatus())
                .statusLabel(spanStatusLabel(agentSpan.getStatus()))
                .durationMs(agentSpan.getDurationMs())
                .durationLabel(formatDuration(agentSpan.getDurationMs()))
                .errorMessage(agentSpan.getErrorMessage())
                .startedAt(agentSpan.getStartedAt())
                .build();
        return toDetailVO(agentSpan, List.of(chatNode));
    }

    public List<TraceNodeVO> listSpanNodes(String traceId) {
        return getSpanDetail(traceId).getNodes();
    }

    public ObservabilityOverviewVO getObservabilityOverview() {
        Long tenantId = requireTenantId();
        long todayCalls = safeLong(monitorStatsMapper.countCallsToday(tenantId));
        long failedCalls = safeLong(traceMapper.countFailedCallsToday(tenantId));
        long avgLatency = safeLong(monitorStatsMapper.avgLatencyToday(tenantId));
        int failureRate = todayCalls > 0 ? (int) Math.round(failedCalls * 100.0 / todayCalls) : 0;

        return ObservabilityOverviewVO.builder()
                .metrics(List.of(
                        metric("failureRate", "今日失败率", failureRate + "%", "基于 Token 调用统计"),
                        metric("errors", "今日错误数", String.valueOf(failedCalls), "失败调用次数"),
                        metric("latency", "平均延迟", formatDuration((int) avgLatency), "今日成功调用"),
                        metric("calls", "今日调用", formatCount(todayCalls), "自今日 00:00 起")
                ))
                .services(infrastructureHealthChecker.checkAll())
                .failedTrend(toTrend(traceMapper.hourlyFailedTrend(tenantId)))
                .latencyTrend(toLatencyTrend(traceMapper.hourlyLatencyTrend(tenantId)))
                .build();
    }

    private TraceSpanVO toSpanVO(TraceSpanRow row) {
        return TraceSpanVO.builder()
                .traceId(row.getTraceId())
                .spanType(row.getSpanType())
                .spanTypeLabel(spanTypeLabel(row.getSpanType()))
                .name(row.getName())
                .status(row.getStatus())
                .statusLabel(spanStatusLabel(row.getStatus()))
                .durationMs(row.getDurationMs())
                .durationLabel(formatDuration(row.getDurationMs()))
                .startedAt(row.getStartedAt())
                .errorMessage(row.getErrorMessage())
                .build();
    }

    private TraceDetailVO toDetailVO(TraceSpanRow row, List<TraceNodeVO> nodes) {
        return TraceDetailVO.builder()
                .traceId(row.getTraceId())
                .spanType(row.getSpanType())
                .spanTypeLabel(spanTypeLabel(row.getSpanType()))
                .name(row.getName())
                .status(row.getStatus())
                .statusLabel(spanStatusLabel(row.getStatus()))
                .durationMs(row.getDurationMs())
                .durationLabel(formatDuration(row.getDurationMs()))
                .startedAt(row.getStartedAt())
                .finishedAt(row.getFinishedAt())
                .errorMessage(row.getErrorMessage())
                .nodes(nodes)
                .build();
    }

    private TraceNodeVO toNodeVO(TraceNodeRow row) {
        return TraceNodeVO.builder()
                .nodeId(row.getNodeId())
                .nodeName(StringUtils.hasText(row.getNodeName()) ? row.getNodeName() : row.getNodeId())
                .nodeType(row.getNodeType())
                .status(row.getStatus())
                .statusLabel(nodeStatusLabel(row.getStatus()))
                .durationMs(row.getDurationMs())
                .durationLabel(formatDuration(row.getDurationMs()))
                .errorMessage(row.getErrorMessage())
                .startedAt(row.getStartedAt())
                .build();
    }

    private LocalDateTime resolveSince(String timeRange) {
        if (!StringUtils.hasText(timeRange)) {
            return LocalDateTime.now().minusHours(24);
        }
        return switch (timeRange.trim()) {
            case "1h" -> LocalDateTime.now().minusHours(1);
            case "7d" -> LocalDateTime.now().minusDays(7);
            default -> LocalDateTime.now().minusHours(24);
        };
    }

    private String spanTypeLabel(String spanType) {
        return "workflow".equals(spanType) ? "工作流" : "Agent";
    }

    private String spanStatusLabel(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "运行中";
            case 1 -> "成功";
            case 2 -> "失败";
            case 3 -> "超时";
            default -> "未知";
        };
    }

    private String nodeStatusLabel(Integer status) {
        if (status == null) {
            return "待执行";
        }
        return switch (status) {
            case 0 -> "运行中";
            case 1 -> "完成";
            case 2 -> "失败";
            default -> "待执行";
        };
    }

    private List<MonitorOverviewVO.TrendPointVO> toTrend(List<TrendPointRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .map(row -> MonitorOverviewVO.TrendPointVO.builder()
                        .time(row.getTimeLabel())
                        .value(safeLong(row.getValue()))
                        .build())
                .toList();
    }

    private List<MonitorOverviewVO.TrendPointVO> toLatencyTrend(List<TrendPointRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .map(row -> MonitorOverviewVO.TrendPointVO.builder()
                        .time(row.getTimeLabel())
                        .value(safeLong(row.getValue()))
                        .build())
                .toList();
    }

    private MonitorOverviewVO.MetricCardVO metric(String key, String label, String value, String hint) {
        return MonitorOverviewVO.MetricCardVO.builder()
                .key(key)
                .label(label)
                .value(value)
                .hint(hint)
                .build();
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
}
