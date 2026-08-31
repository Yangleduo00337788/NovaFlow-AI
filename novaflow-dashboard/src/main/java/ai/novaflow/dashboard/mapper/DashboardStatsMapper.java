package ai.novaflow.dashboard.mapper;

import ai.novaflow.dashboard.domain.DailySparklineRow;
import ai.novaflow.dashboard.domain.NamedCountRow;
import ai.novaflow.dashboard.domain.PublishedWorkflowRow;
import ai.novaflow.dashboard.domain.RecentUsageLogRow;
import ai.novaflow.dashboard.domain.TrendPointRow;
import ai.novaflow.dashboard.domain.WorkflowEdgeRow;
import ai.novaflow.dashboard.domain.WorkflowNodeLogRow;
import ai.novaflow.dashboard.domain.WorkflowNodeRow;
import ai.novaflow.dashboard.domain.WorkflowRuntimeRow;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DashboardStatsMapper {

    Long countApplications(Long tenantId);

    Long countAgents(Long tenantId);

    Long countKnowledgeBases(Long tenantId);

    List<RecentUsageLogRow> recentUsageLogs(Long tenantId, int limit);

    List<NamedCountRow> topApplications(Long tenantId);

    WorkflowRuntimeRow latestPublishedWorkflow(Long tenantId);

    List<PublishedWorkflowRow> listPublishedWorkflows(Long tenantId, int limit);

    WorkflowRuntimeRow latestExecutionForWorkflow(Long tenantId, Long workflowId);

    WorkflowRuntimeRow findWorkflow(Long tenantId, Long workflowId);

    List<WorkflowNodeRow> workflowNodes(Long tenantId, Long workflowId);

    String workflowCanvasData(Long tenantId, Long workflowId);

    List<WorkflowEdgeRow> workflowEdges(Long tenantId, Long workflowId);

    List<WorkflowNodeLogRow> workflowNodeLogs(String executionId);

    List<TrendPointRow> hourlyTrend(Long tenantId);

    List<DailySparklineRow> dailyCostSparkline(Long tenantId, String currency);

    List<DailySparklineRow> dailyInvocationSparkline(Long tenantId);

    List<DailySparklineRow> dailyTokenSparkline(Long tenantId);

    Long countApplicationsBeforeDays(Long tenantId, int days);

    Long countAgentsBeforeDays(Long tenantId, int days);

    Long countKnowledgeBasesBeforeDays(Long tenantId, int days);

    List<DailySparklineRow> dailyApplicationSparkline(Long tenantId);

    List<DailySparklineRow> dailyAgentSparkline(Long tenantId);

    List<DailySparklineRow> dailyKnowledgeSparkline(Long tenantId);
}
