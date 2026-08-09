package ai.novaflow.dashboard.mapper;

import ai.novaflow.dashboard.domain.DailySparklineRow;
import ai.novaflow.dashboard.domain.NamedCountRow;
import ai.novaflow.dashboard.domain.PublishedWorkflowRow;
import ai.novaflow.dashboard.domain.RecentUsageLogRow;
import ai.novaflow.dashboard.domain.TrendPointRow;
import ai.novaflow.dashboard.domain.WorkflowNodeLogRow;
import ai.novaflow.dashboard.domain.WorkflowEdgeRow;
import ai.novaflow.dashboard.domain.WorkflowNodeRow;
import ai.novaflow.dashboard.domain.WorkflowRuntimeRow;
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
            SELECT tu.id,
                   COALESCE(a.agent_name, '未知 Agent') AS agent_name,
                   tu.total_tokens,
                   tu.latency_ms,
                   tu.created_at,
                   tu.success,
                   tu.trace_id
            FROM token_usage tu
            LEFT JOIN agent a ON tu.agent_id = a.id
            WHERE tu.tenant_id = #{tenantId}
            ORDER BY tu.created_at DESC
            LIMIT #{limit}
            """)
    List<RecentUsageLogRow> recentUsageLogs(Long tenantId, int limit);

    @Select("""
            SELECT COALESCE(app.app_name, '未命名应用') AS name,
                   COUNT(*) AS value
            FROM token_usage tu
            LEFT JOIN application app ON tu.application_id = app.id
            WHERE tu.tenant_id = #{tenantId}
            GROUP BY tu.application_id, app.app_name
            ORDER BY value DESC
            LIMIT 5
            """)
    List<NamedCountRow> topApplications(Long tenantId);

    @Select("""
            SELECT NULL AS execution_id,
                   w.id AS workflow_id,
                   w.status,
                   w.workflow_name
            FROM workflow w
            WHERE w.tenant_id = #{tenantId}
              AND w.is_deleted = 0
              AND w.status = 1
            ORDER BY w.created_at DESC
            LIMIT 1
            """)
    WorkflowRuntimeRow latestPublishedWorkflow(Long tenantId);

    @Select("""
            SELECT w.id AS workflow_id,
                   w.workflow_name,
                   w.status,
                   COALESCE(app.app_name, '未命名应用') AS application_name,
                   w.updated_at
            FROM workflow w
            LEFT JOIN application app ON w.application_id = app.id AND app.is_deleted = 0
            WHERE w.tenant_id = #{tenantId}
              AND w.is_deleted = 0
              AND w.status = 1
            ORDER BY w.created_at DESC
            LIMIT #{limit}
            """)
    List<PublishedWorkflowRow> listPublishedWorkflows(Long tenantId, int limit);

    @Select("""
            SELECT we.execution_id,
                   we.workflow_id,
                   we.status,
                   w.workflow_name
            FROM workflow_execution we
            INNER JOIN workflow w ON we.workflow_id = w.id AND w.is_deleted = 0
            WHERE we.tenant_id = #{tenantId}
            ORDER BY CASE WHEN we.status = 0 THEN 0 ELSE 1 END, we.started_at DESC
            LIMIT 1
            """)
    WorkflowRuntimeRow latestWorkflowExecution(Long tenantId);

    @Select("""
            SELECT we.execution_id,
                   we.workflow_id,
                   we.status,
                   w.workflow_name
            FROM workflow_execution we
            INNER JOIN workflow w ON we.workflow_id = w.id AND w.is_deleted = 0
            WHERE we.tenant_id = #{tenantId}
              AND we.workflow_id = #{workflowId}
            ORDER BY we.started_at DESC
            LIMIT 1
            """)
    WorkflowRuntimeRow latestExecutionForWorkflow(Long tenantId, Long workflowId);

    @Select("""
            SELECT w.id AS workflow_id,
                   w.workflow_name,
                   w.status,
                   NULL AS execution_id
            FROM workflow w
            WHERE w.tenant_id = #{tenantId}
              AND w.id = #{workflowId}
              AND w.is_deleted = 0
            LIMIT 1
            """)
    WorkflowRuntimeRow findWorkflow(Long tenantId, Long workflowId);

    @Select("""
            SELECT node_id, node_name, node_type, sort_order, position_x, position_y
            FROM workflow_node
            WHERE workflow_id = #{workflowId}
            ORDER BY sort_order, id
            """)
    List<WorkflowNodeRow> workflowNodes(Long workflowId);

    @Select("""
            SELECT canvas_data
            FROM workflow
            WHERE id = #{workflowId} AND is_deleted = 0
            LIMIT 1
            """)
    String workflowCanvasData(Long workflowId);

    @Select("""
            SELECT edge_id, source_node_id, target_node_id, source_handle, target_handle, `condition` AS condition_expr
            FROM workflow_edge
            WHERE workflow_id = #{workflowId}
            ORDER BY id
            """)
    List<WorkflowEdgeRow> workflowEdges(Long workflowId);

    @Select("""
            SELECT node_id, status
            FROM workflow_node_log
            WHERE execution_id = #{executionId}
            """)
    List<WorkflowNodeLogRow> workflowNodeLogs(String executionId);

    @Select("""
            SELECT CONCAT(LPAD(HOUR(MIN(created_at)), 2, '0'), ':00') AS time_label,
                   COUNT(*) AS value
            FROM token_usage
            WHERE tenant_id = #{tenantId}
              AND created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
            GROUP BY HOUR(created_at)
            ORDER BY HOUR(created_at)
            """)
    List<TrendPointRow> hourlyTrend(Long tenantId);

    @Select("""
            SELECT COALESCE(SUM(cost), 0) FROM token_usage
            WHERE tenant_id = #{tenantId} AND currency = #{currency}
              AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
            """)
    BigDecimal sumCostLast7Days(Long tenantId, String currency);

    @Select("""
            SELECT COALESCE(SUM(cost), 0) FROM token_usage
            WHERE tenant_id = #{tenantId} AND currency = #{currency}
              AND created_at >= DATE_SUB(NOW(), INTERVAL 14 DAY)
              AND created_at < DATE_SUB(NOW(), INTERVAL 7 DAY)
            """)
    BigDecimal sumCostPrev7Days(Long tenantId, String currency);

    @Select("""
            SELECT DATE_FORMAT(MIN(created_at), '%m-%d') AS day_label,
                   CAST(ROUND(COALESCE(SUM(cost), 0) * 100) AS SIGNED) AS value
            FROM token_usage
            WHERE tenant_id = #{tenantId} AND currency = #{currency}
              AND created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
            GROUP BY DATE(created_at)
            ORDER BY DATE(created_at)
            """)
    List<DailySparklineRow> dailyCostSparkline(Long tenantId, String currency);

    @Select("""
            SELECT DATE_FORMAT(MIN(created_at), '%m-%d') AS day_label,
                   COUNT(*) AS value
            FROM token_usage
            WHERE tenant_id = #{tenantId}
              AND created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
            GROUP BY DATE(created_at)
            ORDER BY DATE(created_at)
            """)
    List<DailySparklineRow> dailyInvocationSparkline(Long tenantId);

    @Select("""
            SELECT DATE_FORMAT(MIN(created_at), '%m-%d') AS day_label,
                   COALESCE(SUM(total_tokens), 0) AS value
            FROM token_usage
            WHERE tenant_id = #{tenantId}
              AND created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
            GROUP BY DATE(created_at)
            ORDER BY DATE(created_at)
            """)
    List<DailySparklineRow> dailyTokenSparkline(Long tenantId);

    @Select("""
            SELECT COUNT(*) FROM application
            WHERE tenant_id = #{tenantId} AND is_deleted = 0
              AND created_at < DATE_SUB(CURDATE(), INTERVAL #{days} DAY)
            """)
    Long countApplicationsBeforeDays(Long tenantId, int days);

    @Select("""
            SELECT COUNT(*) FROM agent
            WHERE tenant_id = #{tenantId} AND is_deleted = 0
              AND created_at < DATE_SUB(CURDATE(), INTERVAL #{days} DAY)
            """)
    Long countAgentsBeforeDays(Long tenantId, int days);

    @Select("""
            SELECT COUNT(*) FROM knowledge_base
            WHERE tenant_id = #{tenantId} AND is_deleted = 0
              AND created_at < DATE_SUB(CURDATE(), INTERVAL #{days} DAY)
            """)
    Long countKnowledgeBasesBeforeDays(Long tenantId, int days);

    @Select("""
            SELECT DATE(MIN(created_at)) AS stat_date,
                   DATE_FORMAT(MIN(created_at), '%m-%d') AS day_label,
                   COUNT(*) AS value
            FROM application
            WHERE tenant_id = #{tenantId} AND is_deleted = 0
              AND created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
            GROUP BY DATE(created_at)
            ORDER BY DATE(created_at)
            """)
    List<DailySparklineRow> dailyApplicationSparkline(Long tenantId);

    @Select("""
            SELECT DATE(MIN(created_at)) AS stat_date,
                   DATE_FORMAT(MIN(created_at), '%m-%d') AS day_label,
                   COUNT(*) AS value
            FROM agent
            WHERE tenant_id = #{tenantId} AND is_deleted = 0
              AND created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
            GROUP BY DATE(created_at)
            ORDER BY DATE(created_at)
            """)
    List<DailySparklineRow> dailyAgentSparkline(Long tenantId);

    @Select("""
            SELECT DATE(MIN(created_at)) AS stat_date,
                   DATE_FORMAT(MIN(created_at), '%m-%d') AS day_label,
                   COUNT(*) AS value
            FROM knowledge_base
            WHERE tenant_id = #{tenantId} AND is_deleted = 0
              AND created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
            GROUP BY DATE(created_at)
            ORDER BY DATE(created_at)
            """)
    List<DailySparklineRow> dailyKnowledgeSparkline(Long tenantId);
}
