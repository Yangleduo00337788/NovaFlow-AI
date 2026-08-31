package ai.novaflow.monitor.mapper;

import ai.novaflow.monitor.domain.NamedCountRow;
import ai.novaflow.monitor.domain.TrendPointRow;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MonitorStatsMapper {

    Long countCallsToday(Long tenantId);

    Long sumTokensToday(Long tenantId);

    Long countActiveAgents24h(Long tenantId);

    Long avgLatencyToday(Long tenantId);

    Long countPublishedAgents(Long tenantId);

    List<NamedCountRow> topAgents(Long tenantId);

    List<NamedCountRow> topApplications(Long tenantId);

    List<TrendPointRow> hourlyInvocationTrend(Long tenantId);

    Long p95LatencyToday(Long tenantId);

    Long p99LatencyToday(Long tenantId);

    List<NamedCountRow> topErrorAgentsToday(Long tenantId);
}
