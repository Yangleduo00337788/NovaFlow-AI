package ai.novaflow.observability.mapper;

import ai.novaflow.monitor.domain.TrendPointRow;
import ai.novaflow.observability.domain.TraceNodeRow;
import ai.novaflow.observability.domain.TraceSpanRow;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TraceMapper {

    List<TraceSpanRow> pageWorkflowSpans(
            Long tenantId,
            LocalDateTime since,
            Integer status,
            String keyword,
            int offset,
            int pageSize);

    List<TraceSpanRow> pageAgentSpans(
            Long tenantId,
            LocalDateTime since,
            Integer status,
            String keyword,
            int offset,
            int pageSize);

    List<TraceSpanRow> pageAllSpans(
            Long tenantId,
            LocalDateTime since,
            Integer status,
            String keyword,
            int offset,
            int pageSize);

    Long countWorkflowSpans(
            Long tenantId,
            LocalDateTime since,
            Integer status,
            String keyword);

    Long countAgentSpans(
            Long tenantId,
            LocalDateTime since,
            Integer status,
            String keyword);

    TraceSpanRow findWorkflowSpan(Long tenantId, String traceId);

    TraceSpanRow findAgentSpanById(Long tenantId, Long usageId);

    TraceSpanRow findAgentSpanByTraceId(Long tenantId, String traceId);

    List<TraceNodeRow> workflowNodes(Long tenantId, String traceId);

    Long countFailedCallsToday(Long tenantId);

    List<TrendPointRow> hourlyFailedTrend(Long tenantId);

    List<TrendPointRow> hourlyLatencyTrend(Long tenantId);
}
