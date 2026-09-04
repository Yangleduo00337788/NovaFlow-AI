package ai.novaflow.monitor.service;
import ai.novaflow.common.context.TenantContexts;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.monitor.domain.NamedCountRow;
import ai.novaflow.monitor.domain.TrendPointRow;
import ai.novaflow.monitor.domain.vo.MonitorOverviewVO;
import ai.novaflow.monitor.mapper.MonitorStatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MonitorService {

    private final MonitorStatsMapper monitorStatsMapper;
    private final InfrastructureHealthChecker infrastructureHealthChecker;

    public MonitorOverviewVO getOverview() {
        Long tenantId = TenantContexts.requireTenantId();

        long todayCalls = safeLong(monitorStatsMapper.countCallsToday(tenantId));
        long todayTokens = safeLong(monitorStatsMapper.sumTokensToday(tenantId));
        long activeAgents = safeLong(monitorStatsMapper.countActiveAgents24h(tenantId));
        long avgLatency = safeLong(monitorStatsMapper.avgLatencyToday(tenantId));
        long publishedAgents = safeLong(monitorStatsMapper.countPublishedAgents(tenantId));

        return MonitorOverviewVO.builder()
                .metrics(List.of(
                        metric("calls", "今日调用", formatCount(todayCalls), "自今日 00:00 起"),
                        metric("tokens", "今日 Token", formatCount(todayTokens), "累计消耗"),
                        metric("activeAgents", "活跃 Agent", String.valueOf(activeAgents), "近 24 小时有调用"),
                        metric("latency", "平均延迟", formatLatency(avgLatency), "今日成功调用"),
                        metric("publishedAgents", "已发布 Agent", String.valueOf(publishedAgents), "当前租户")
                ))
                .services(infrastructureHealthChecker.checkAll())
                .topAgents(toRanking(monitorStatsMapper.topAgents(tenantId), "次"))
                .topApplications(toRanking(monitorStatsMapper.topApplications(tenantId), "次"))
                .hourlyTrend(toTrend(monitorStatsMapper.hourlyInvocationTrend(tenantId)))
                .build();
    }

    private List<MonitorOverviewVO.RankingItemVO> toRanking(List<NamedCountRow> rows, String unit) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .map(row -> MonitorOverviewVO.RankingItemVO.builder()
                        .name(row.getName())
                        .value(safeLong(row.getValue()))
                        .valueLabel(formatCount(safeLong(row.getValue())) + unit)
                        .build())
                .toList();
    }

    private List<MonitorOverviewVO.TrendPointVO> toTrend(List<TrendPointRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of(
                    trend("00:00", 0L), trend("06:00", 0L), trend("12:00", 0L),
                    trend("18:00", 0L), trend("23:00", 0L)
            );
        }
        return rows.stream()
                .map(row -> trend(row.getTimeLabel(), safeLong(row.getValue())))
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

    private MonitorOverviewVO.TrendPointVO trend(String time, Long value) {
        return MonitorOverviewVO.TrendPointVO.builder().time(time).value(value).build();
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

    private String formatLatency(long latencyMs) {
        if (latencyMs <= 0) {
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

}
