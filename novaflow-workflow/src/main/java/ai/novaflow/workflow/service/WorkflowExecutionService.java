package ai.novaflow.workflow.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.model.domain.dto.ModelUsageRecordRequest;
import ai.novaflow.model.service.ModelUsageService;
import ai.novaflow.workflow.domain.WorkflowExecutionStatus;
import ai.novaflow.workflow.domain.dto.WorkflowRunOptions;
import ai.novaflow.workflow.domain.dto.WorkflowRunRequest;
import ai.novaflow.workflow.domain.vo.WorkflowModelUsageVO;
import ai.novaflow.workflow.domain.vo.WorkflowRunResultVO;
import ai.novaflow.workflow.domain.vo.WorkflowRunStepVO;
import ai.novaflow.workflow.entity.WorkflowEdgeEntity;
import ai.novaflow.workflow.entity.WorkflowEntity;
import ai.novaflow.workflow.entity.WorkflowExecutionEntity;
import ai.novaflow.workflow.entity.WorkflowNodeEntity;
import ai.novaflow.workflow.entity.WorkflowNodeLogEntity;
import ai.novaflow.workflow.mapper.WorkflowEdgeMapper;
import ai.novaflow.workflow.mapper.WorkflowExecutionMapper;
import ai.novaflow.workflow.mapper.WorkflowMapper;
import ai.novaflow.workflow.mapper.WorkflowNodeLogMapper;
import ai.novaflow.workflow.mapper.WorkflowNodeMapper;
import ai.novaflow.workflow.util.WorkflowElBuilder;
import ai.novaflow.workflowengine.WorkflowLiteFlowExecutor;
import ai.novaflow.workflowengine.domain.WorkflowExecutionContext;
import ai.novaflow.workflowengine.domain.WorkflowNodeDefinition;
import ai.novaflow.workflowengine.domain.WorkflowStepSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkflowExecutionService {

    private final WorkflowMapper workflowMapper;
    private final WorkflowNodeMapper workflowNodeMapper;
    private final WorkflowEdgeMapper workflowEdgeMapper;
    private final WorkflowExecutionMapper workflowExecutionMapper;
    private final WorkflowNodeLogMapper workflowNodeLogMapper;
    private final ModelUsageService modelUsageService;
    private final ObjectMapper objectMapper;
    private final WorkflowNodeProcessorImpl workflowNodeProcessor;
    private final WorkflowLiteFlowExecutor workflowLiteFlowExecutor;

    @Transactional
    public WorkflowRunResultVO run(Long workflowId, WorkflowRunRequest request) {
        return run(workflowId, request, WorkflowRunOptions.builder().build());
    }

    @Transactional
    public WorkflowRunResultVO run(Long workflowId, WorkflowRunRequest request, Long triggeredByUserId) {
        return run(workflowId, request, WorkflowRunOptions.builder().triggeredByUserId(triggeredByUserId).build());
    }

    @Transactional
    public WorkflowRunResultVO run(Long workflowId, WorkflowRunRequest request, WorkflowRunOptions options) {
        WorkflowRunOptions runOptions = options != null ? options : WorkflowRunOptions.builder().build();
        Long triggeredByUserId = runOptions.getTriggeredByUserId();
        Long tenantId = requireTenantId();
        WorkflowEntity workflow = getWorkflowOrThrow(workflowId, tenantId);
        List<WorkflowNodeEntity> nodes = listNodes(workflowId, tenantId);
        List<WorkflowEdgeEntity> edges = listEdges(workflowId, tenantId);
        if (nodes.isEmpty()) {
            throw new BusinessException("工作流没有可执行的节点");
        }

        String input = request != null && StringUtils.hasText(request.getInput()) ? request.getInput().trim() : "";
        String executionId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime startedAt = LocalDateTime.now();
        long startedMs = System.currentTimeMillis();

        WorkflowExecutionEntity execution = new WorkflowExecutionEntity();
        execution.setTenantId(tenantId);
        execution.setWorkflowId(workflowId);
        execution.setExecutionId(executionId);
        execution.setStatus(WorkflowExecutionStatus.RUNNING);
        execution.setInputData(writeJson(Map.of("input", input)));
        execution.setStartedAt(startedAt);
        execution.setTriggeredBy(triggeredByUserId);
        workflowExecutionMapper.insert(execution);

        ExecutionOutcome outcome = runWithLiteFlow(nodes, edges, input, tenantId, executionId);

        int durationMs = (int) (System.currentTimeMillis() - startedMs);
        execution.setStatus(outcome.status());
        execution.setOutputData(writeJson(Map.of("output", outcome.output() != null ? outcome.output() : "")));
        execution.setErrorMessage(outcome.errorMessage());
        execution.setFinishedAt(LocalDateTime.now());
        execution.setDurationMs(durationMs);
        workflowExecutionMapper.update(execution);

        if (runOptions.isRecordUsage()) {
            recordModelUsages(workflow, triggeredByUserId, runOptions.getAgentId(), outcome.modelUsages());
        }

        return WorkflowRunResultVO.builder()
                .executionId(executionId)
                .status(outcome.status())
                .output(outcome.output())
                .errorMessage(outcome.errorMessage())
                .durationMs(durationMs)
                .tokensUsed(outcome.totalTokens())
                .modelUsages(outcome.modelUsages())
                .steps(outcome.steps())
                .build();
    }

    private ExecutionOutcome runWithLiteFlow(
            List<WorkflowNodeEntity> nodes,
            List<WorkflowEdgeEntity> edges,
            String input,
            Long tenantId,
            String executionId) {
        String elExpression = WorkflowElBuilder.build(nodes, edges);
        if (!StringUtils.hasText(elExpression)) {
            throw new BusinessException("无法生成工作流 EL 表达式");
        }

        WorkflowExecutionContext context = new WorkflowExecutionContext();
        context.setTenantId(tenantId);
        context.setPayload(input);
        context.setNodeProcessor(workflowNodeProcessor);
        for (WorkflowNodeEntity node : nodes) {
            context.getNodeMap().put(node.getNodeId(), toDefinition(node));
        }

        workflowLiteFlowExecutor.execute(executionId, elExpression, context);
        persistStepLogs(tenantId, executionId, context.getSteps());

        Integer status = context.isFailed()
                ? WorkflowExecutionStatus.FAILED
                : WorkflowExecutionStatus.SUCCESS;
        List<WorkflowModelUsageVO> modelUsages = extractModelUsages(context.getModelUsages());
        List<WorkflowRunStepVO> steps = toStepVos(context.getSteps());
        String output = context.getPayload();
        return new ExecutionOutcome(status, output, context.getErrorMessage(), context.getTotalTokens(), modelUsages, steps);
    }

    private void persistStepLogs(Long tenantId, String executionId, List<WorkflowStepSnapshot> snapshots) {
        for (WorkflowStepSnapshot snapshot : snapshots) {
            LocalDateTime finishedAt = LocalDateTime.now();
            LocalDateTime startedAt = finishedAt.minusNanos(snapshot.getDurationMs() * 1_000_000L);
            WorkflowNodeLogEntity logEntity = new WorkflowNodeLogEntity();
            logEntity.setTenantId(tenantId);
            logEntity.setExecutionId(executionId);
            logEntity.setNodeId(snapshot.getNodeId());
            logEntity.setNodeType(snapshot.getNodeType());
            logEntity.setStatus(snapshot.getStatus());
            logEntity.setInputData(writeJson(Map.of("input", snapshot.getInput())));
            logEntity.setOutputData(writeJson(Map.of("output", snapshot.getOutput())));
            logEntity.setErrorMessage(snapshot.getErrorMessage());
            logEntity.setStartedAt(startedAt);
            logEntity.setFinishedAt(finishedAt);
            logEntity.setDurationMs(snapshot.getDurationMs());
            workflowNodeLogMapper.insert(logEntity);
        }
    }

    private List<WorkflowRunStepVO> toStepVos(List<WorkflowStepSnapshot> snapshots) {
        return snapshots.stream()
                .map(snapshot -> WorkflowRunStepVO.builder()
                        .nodeId(snapshot.getNodeId())
                        .nodeType(snapshot.getNodeType())
                        .nodeName(snapshot.getNodeName())
                        .status(snapshot.getStatus())
                        .input(snapshot.getInput())
                        .output(snapshot.getOutput())
                        .errorMessage(snapshot.getErrorMessage())
                        .durationMs(snapshot.getDurationMs())
                        .build())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<WorkflowModelUsageVO> extractModelUsages(List<Object> usages) {
        List<WorkflowModelUsageVO> result = new ArrayList<>();
        for (Object usage : usages) {
            if (usage instanceof WorkflowModelUsageVO modelUsage) {
                result.add(modelUsage);
            }
        }
        return result;
    }

    private WorkflowNodeDefinition toDefinition(WorkflowNodeEntity node) {
        return WorkflowNodeDefinition.builder()
                .nodeId(node.getNodeId())
                .nodeType(node.getNodeType())
                .nodeName(node.getNodeName())
                .nodeConfig(node.getNodeConfig())
                .workflowId(node.getWorkflowId())
                .build();
    }

    private WorkflowEntity getWorkflowOrThrow(Long workflowId, Long tenantId) {
        WorkflowEntity entity = workflowMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", workflowId)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
        );
        if (entity == null) {
            throw new BusinessException("工作流不存在");
        }
        return entity;
    }

    private List<WorkflowNodeEntity> listNodes(Long workflowId, Long tenantId) {
        return workflowNodeMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("workflow_id", workflowId)
                        .eq("tenant_id", tenantId)
                        .orderBy("sort_order", true)
        );
    }

    private List<WorkflowEdgeEntity> listEdges(Long workflowId, Long tenantId) {
        return workflowEdgeMapper.selectListByQuery(
                QueryWrapper.create().eq("workflow_id", workflowId).eq("tenant_id", tenantId)
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void recordModelUsages(
            WorkflowEntity workflow,
            Long userId,
            Long agentId,
            List<WorkflowModelUsageVO> modelUsages) {
        if (modelUsages == null || modelUsages.isEmpty()) {
            return;
        }
        for (WorkflowModelUsageVO usage : modelUsages) {
            if (usage.getModelConfigId() == null || safeInt(usage.getTotalTokens()) <= 0) {
                continue;
            }
            modelUsageService.record(ModelUsageRecordRequest.builder()
                    .tenantId(workflow.getTenantId())
                    .applicationId(workflow.getApplicationId())
                    .agentId(agentId)
                    .userId(userId)
                    .modelConfigId(usage.getModelConfigId())
                    .usageType("workflow")
                    .inputTokens(usage.getInputTokens())
                    .outputTokens(usage.getOutputTokens())
                    .totalTokens(usage.getTotalTokens())
                    .latencyMs(usage.getLatencyMs() != null ? usage.getLatencyMs().longValue() : null)
                    .build());
        }
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户上下文缺失");
        }
        return tenantId;
    }

    private record ExecutionOutcome(
            Integer status,
            String output,
            String errorMessage,
            int totalTokens,
            List<WorkflowModelUsageVO> modelUsages,
            List<WorkflowRunStepVO> steps) {
    }
}
