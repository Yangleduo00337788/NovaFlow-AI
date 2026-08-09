package ai.novaflow.monitor.mapper;

import ai.novaflow.monitor.domain.NamedCountRow;
import ai.novaflow.monitor.domain.TrendPointRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MonitorStatsMapper {

    @Select("""
            SELECT COUNT(*) FROM token_usage
            WHERE tenant_id = #{tenantId}
              AND DATE(created_at) = CURDATE()
            """)
    Long countCallsToday(Long tenantId);

    @Select("""
            SELECT COALESCE(SUM(total_tokens), 0) FROM token_usage
            WHERE tenant_id = #{tenantId}
              AND DATE(created_at) = CURDATE()
            """)
    Long sumTokensToday(Long tenantId);

    @Select("""
            SELECT COUNT(DISTINCT agent_id) FROM token_usage
            WHERE tenant_id = #{tenantId}
              AND agent_id IS NOT NULL
              AND created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
            """)
    Long countActiveAgents24h(Long tenantId);

    @Select("""
            SELECT COALESCE(AVG(latency_ms), 0) FROM token_usage
            WHERE tenant_id = #{tenantId}
              AND DATE(created_at) = CURDATE()
              AND latency_ms IS NOT NULL
            """)
    Long avgLatencyToday(Long tenantId);

    @Select("""
            SELECT COUNT(*) FROM agent
            WHERE tenant_id = #{tenantId}
              AND is_deleted = 0
              AND status = 1
            """)
    Long countPublishedAgents(Long tenantId);

    @Select("""
            SELECT COALESCE(a.agent_name, '未知 Agent') AS name,
                   COUNT(*) AS value
            FROM token_usage tu
            LEFT JOIN agent a ON tu.agent_id = a.id
            WHERE tu.tenant_id = #{tenantId}
              AND tu.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
            GROUP BY tu.agent_id, a.agent_name
            ORDER BY value DESC
            LIMIT 8
            """)
    List<NamedCountRow> topAgents(Long tenantId);

    @Select("""
            SELECT COALESCE(app.app_name, '未关联应用') AS name,
                   COUNT(*) AS value
            FROM token_usage tu
            LEFT JOIN application app ON tu.application_id = app.id
            WHERE tu.tenant_id = #{tenantId}
              AND tu.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
            GROUP BY tu.application_id, app.app_name
            ORDER BY value DESC
            LIMIT 8
            """)
    List<NamedCountRow> topApplications(Long tenantId);

    @Select("""
            SELECT CONCAT(LPAD(HOUR(MIN(created_at)), 2, '0'), ':00') AS time_label,
                   COUNT(*) AS value
            FROM token_usage
            WHERE tenant_id = #{tenantId}
              AND created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
            GROUP BY HOUR(created_at)
            ORDER BY HOUR(created_at)
            """)
    List<TrendPointRow> hourlyInvocationTrend(Long tenantId);

    @Select("""
            SELECT COALESCE(latency_ms, 0)
            FROM (
                SELECT latency_ms,
                       ROW_NUMBER() OVER (ORDER BY latency_ms) AS row_num,
                       COUNT(*) OVER () AS total_count
                FROM token_usage
                WHERE tenant_id = #{tenantId}
                  AND DATE(created_at) = CURDATE()
                  AND latency_ms IS NOT NULL
            ) ranked
            WHERE row_num = GREATEST(1, CEILING(total_count * 0.95))
            LIMIT 1
            """)
    Long p95LatencyToday(Long tenantId);

    @Select("""
            SELECT COALESCE(latency_ms, 0)
            FROM (
                SELECT latency_ms,
                       ROW_NUMBER() OVER (ORDER BY latency_ms) AS row_num,
                       COUNT(*) OVER () AS total_count
                FROM token_usage
                WHERE tenant_id = #{tenantId}
                  AND DATE(created_at) = CURDATE()
                  AND latency_ms IS NOT NULL
            ) ranked
            WHERE row_num = GREATEST(1, CEILING(total_count * 0.99))
            LIMIT 1
            """)
    Long p99LatencyToday(Long tenantId);

    @Select("""
            SELECT COALESCE(a.agent_name, '未知 Agent') AS name,
                   COUNT(*) AS value
            FROM token_usage tu
            LEFT JOIN agent a ON tu.agent_id = a.id
            WHERE tu.tenant_id = #{tenantId}
              AND tu.success = 0
              AND DATE(tu.created_at) = CURDATE()
            GROUP BY tu.agent_id, a.agent_name
            ORDER BY value DESC
            LIMIT 5
            """)
    List<NamedCountRow> topErrorAgentsToday(Long tenantId);
}
