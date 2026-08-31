package ai.novaflow.observability.mapper;

import ai.novaflow.observability.domain.TraceNodeRow;
import ai.novaflow.observability.domain.TraceSpanRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TraceMapper {

    @Select("""
            <script>
            SELECT * FROM (
            SELECT we.execution_id AS trace_id,
                   'workflow' AS span_type,
                   w.workflow_name AS name,
                   we.status,
                   we.duration_ms,
                   we.started_at,
                   we.finished_at,
                   we.error_message
                FROM workflow_execution we
                INNER JOIN workflow w ON we.workflow_id = w.id AND w.is_deleted = 0
                WHERE we.tenant_id = #{tenantId}
                <if test="since != null">AND we.started_at &gt;= #{since}</if>
                UNION ALL
                SELECT COALESCE(NULLIF(tu.trace_id, ''), CONCAT('agent-', tu.id)) AS trace_id,
                       'agent' AS span_type,
                       COALESCE(a.agent_name, '未知 Agent') AS name,
                       CASE WHEN tu.success IS NULL OR tu.success = 1 THEN 1 ELSE 2 END AS status,
                       tu.latency_ms AS duration_ms,
                       tu.created_at AS started_at,
                       tu.created_at AS finished_at,
                       tu.error_message
                FROM token_usage tu
                LEFT JOIN agent a ON tu.agent_id = a.id
                WHERE tu.tenant_id = #{tenantId}
                <if test="since != null">AND tu.created_at &gt;= #{since}</if>
            ) spans
            WHERE 1 = 1
            <if test="spanType != null and spanType != ''">AND span_type = #{spanType}</if>
            <if test="status != null">AND status = #{status}</if>
            <if test="keyword != null and keyword != ''">
              AND (trace_id LIKE CONCAT('%', #{keyword}, '%')
                   OR name LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY started_at DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<TraceSpanRow> pageSpans(
            Long tenantId,
            LocalDateTime since,
            String spanType,
            Integer status,
            String keyword,
            int offset,
            int pageSize);

    @Select("""
            <script>
            SELECT COUNT(*) FROM (
                SELECT we.execution_id AS trace_id,
                       'workflow' AS span_type,
                       w.workflow_name AS name,
                       we.status,
                       we.started_at
                FROM workflow_execution we
                INNER JOIN workflow w ON we.workflow_id = w.id AND w.is_deleted = 0
                WHERE we.tenant_id = #{tenantId}
                <if test="since != null">AND we.started_at &gt;= #{since}</if>
                UNION ALL
                SELECT COALESCE(NULLIF(tu.trace_id, ''), CONCAT('agent-', tu.id)) AS trace_id,
                       'agent' AS span_type,
                       COALESCE(a.agent_name, '未知 Agent') AS name,
                       CASE WHEN tu.success IS NULL OR tu.success = 1 THEN 1 ELSE 2 END AS status,
                       tu.created_at AS started_at
                FROM token_usage tu
                LEFT JOIN agent a ON tu.agent_id = a.id
                WHERE tu.tenant_id = #{tenantId}
                <if test="since != null">AND tu.created_at &gt;= #{since}</if>
            ) spans
            WHERE 1 = 1
            <if test="spanType != null and spanType != ''">AND span_type = #{spanType}</if>
            <if test="status != null">AND status = #{status}</if>
            <if test="keyword != null and keyword != ''">
              AND (trace_id LIKE CONCAT('%', #{keyword}, '%')
                   OR name LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            </script>
            """)
    Long countSpans(
            Long tenantId,
            LocalDateTime since,
            String spanType,
            Integer status,
            String keyword);

    @Select("""
            SELECT we.execution_id AS trace_id,
                   'workflow' AS span_type,
                   w.workflow_name AS name,
                   we.status,
                   we.duration_ms,
                   we.started_at,
                   we.finished_at,
                   we.error_message
            FROM workflow_execution we
            INNER JOIN workflow w ON we.workflow_id = w.id AND w.is_deleted = 0
            WHERE we.tenant_id = #{tenantId}
              AND we.execution_id = #{traceId}
            LIMIT 1
            """)
    TraceSpanRow findWorkflowSpan(Long tenantId, String traceId);

    @Select("""
            SELECT COALESCE(NULLIF(tu.trace_id, ''), CONCAT('agent-', tu.id)) AS trace_id,
                   'agent' AS span_type,
                   COALESCE(a.agent_name, '未知 Agent') AS name,
                   CASE WHEN tu.success IS NULL OR tu.success = 1 THEN 1 ELSE 2 END AS status,
                   tu.latency_ms AS duration_ms,
                   tu.created_at AS started_at,
                   tu.created_at AS finished_at,
                   tu.error_message
            FROM token_usage tu
            LEFT JOIN agent a ON tu.agent_id = a.id
            WHERE tu.tenant_id = #{tenantId}
              AND (tu.trace_id = #{traceId} OR CONCAT('agent-', tu.id) = #{traceId})
            ORDER BY tu.created_at DESC
            LIMIT 1
            """)
    TraceSpanRow findAgentSpan(Long tenantId, String traceId);

    @Select("""
            SELECT wnl.node_id,
                   COALESCE(wn.node_name, wnl.node_id) AS node_name,
                   wnl.node_type,
                   wnl.status,
                   wnl.duration_ms,
                   wnl.error_message,
                   wnl.started_at,
                   wnl.finished_at
            FROM workflow_node_log wnl
            LEFT JOIN workflow_execution we ON wnl.execution_id = we.execution_id
            LEFT JOIN workflow_node wn ON wn.workflow_id = we.workflow_id AND wn.node_id = wnl.node_id
            WHERE wnl.tenant_id = #{tenantId}
              AND wnl.execution_id = #{traceId}
            ORDER BY wnl.started_at, wnl.id
            """)
    List<TraceNodeRow> workflowNodes(Long tenantId, String traceId);

    @Select("""
            SELECT COUNT(*) FROM token_usage
            WHERE tenant_id = #{tenantId}
              AND DATE(created_at) = CURDATE()
              AND success = 0
            """)
    Long countFailedCallsToday(Long tenantId);

    @Select("""
            SELECT CONCAT(LPAD(HOUR(MIN(created_at)), 2, '0'), ':00') AS time_label,
                   COUNT(*) AS value
            FROM token_usage
            WHERE tenant_id = #{tenantId}
              AND success = 0
              AND created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
            GROUP BY HOUR(created_at)
            ORDER BY HOUR(created_at)
            """)
    List<ai.novaflow.monitor.domain.TrendPointRow> hourlyFailedTrend(Long tenantId);

    @Select("""
            SELECT CONCAT(LPAD(HOUR(MIN(created_at)), 2, '0'), ':00') AS time_label,
                   COALESCE(AVG(latency_ms), 0) AS value
            FROM token_usage
            WHERE tenant_id = #{tenantId}
              AND latency_ms IS NOT NULL
              AND created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
            GROUP BY HOUR(created_at)
            ORDER BY HOUR(created_at)
            """)
    List<ai.novaflow.monitor.domain.TrendPointRow> hourlyLatencyTrend(Long tenantId);
}
