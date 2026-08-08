package ai.novaflow.dashboard.mapper;

import ai.novaflow.dashboard.domain.NamedCountRow;
import ai.novaflow.dashboard.domain.RecentUsageLogRow;
import ai.novaflow.dashboard.domain.TrendPointRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface DashboardStatsMapper {

    @Select("""
            SELECT COUNT(*) FROM application WHERE tenant_id = #{tenantId} AND is_deleted = 0
            """)
    Long countApplications(Long tenantId);

    @Select("""
            SELECT COUNT(*) FROM agent WHERE tenant_id = #{tenantId} AND is_deleted = 0
            """)
    Long countAgents(Long tenantId);

    @Select("""
            SELECT COUNT(*) FROM knowledge_base WHERE tenant_id = #{tenantId} AND is_deleted = 0
            """)
    Long countKnowledgeBases(Long tenantId);

    @Select("""
            SELECT COUNT(*) FROM token_usage
            WHERE tenant_id = #{tenantId}
              AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
            """)
    Long countCallsLast7Days(Long tenantId);

    @Select("""
            SELECT COUNT(*) FROM token_usage
            WHERE tenant_id = #{tenantId}
              AND created_at >= DATE_SUB(NOW(), INTERVAL 14 DAY)
              AND created_at < DATE_SUB(NOW(), INTERVAL 7 DAY)
            """)
    Long countCallsPrev7Days(Long tenantId);

    @Select("""
            SELECT COALESCE(SUM(total_tokens), 0) FROM token_usage
            WHERE tenant_id = #{tenantId}
              AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
            """)
    Long sumTokensLast7Days(Long tenantId);

    @Select("""
            SELECT COALESCE(SUM(total_tokens), 0) FROM token_usage
            WHERE tenant_id = #{tenantId}
              AND created_at >= DATE_SUB(NOW(), INTERVAL 14 DAY)
              AND created_at < DATE_SUB(NOW(), INTERVAL 7 DAY)
            """)
    Long sumTokensPrev7Days(Long tenantId);

    @Select("""
            SELECT COALESCE(SUM(cost), 0) FROM token_usage
            WHERE tenant_id = #{tenantId} AND currency = #{currency}
            """)
    BigDecimal sumCostByTenantAndCurrency(Long tenantId, String currency);

    @Select("""
            SELECT COALESCE(a.agent_name, '未知 Agent') AS agent_name,
                   tu.total_tokens,
                   tu.latency_ms,
                   tu.created_at
            FROM token_usage tu
            LEFT JOIN agent a ON tu.agent_id = a.id
            WHERE tu.tenant_id = #{tenantId}
            ORDER BY tu.created_at DESC
            LIMIT #{limit}
            """)
    List<RecentUsageLogRow> recentUsageLogs(Long tenantId, int limit);

    @Select("""
            SELECT COALESCE(a.agent_name, '未知 Agent') AS name,
                   COUNT(*) AS value
            FROM token_usage tu
            LEFT JOIN agent a ON tu.agent_id = a.id
            WHERE tu.tenant_id = #{tenantId}
            GROUP BY tu.agent_id, a.agent_name
            ORDER BY value DESC
            LIMIT 5
            """)
    List<NamedCountRow> topAgents(Long tenantId);

    @Select("""
            SELECT DATE_FORMAT(created_at, '%H:00') AS time_label,
                   COALESCE(SUM(total_tokens), 0) AS value
            FROM token_usage
            WHERE tenant_id = #{tenantId}
              AND created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
            GROUP BY HOUR(created_at)
            ORDER BY HOUR(created_at)
            """)
    List<TrendPointRow> hourlyTrend(Long tenantId);
}
