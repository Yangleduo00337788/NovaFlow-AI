package ai.novaflow.workflow.service;

import ai.novaflow.aiengine.agent.ChatAgentExecutor;
import ai.novaflow.aiengine.agent.ChatExecuteRequest;
import ai.novaflow.aiengine.agent.ChatExecuteResult;
import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.model.domain.ResolvedModelConfig;
import ai.novaflow.model.service.ModelResolutionService;
import ai.novaflow.workflow.domain.WorkflowExecutionStatus;
import ai.novaflow.workflow.domain.WorkflowNodeType;
import ai.novaflow.workflow.domain.dto.WorkflowRunRequest;
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
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowExecutionService {

    private final WorkflowMapper workflowMapper;
    private final WorkflowNodeMapper workflowNodeMapper;
    private final WorkflowEdgeMapper workflowEdgeMapper;
    private final WorkflowExecutionMapper workflowExecutionMapper;
    private final WorkflowNodeLogMapper workflowNodeLogMapper;
    private final ModelResolutionService modelResolutionService;
    private final ChatAgentExecutor chatAgentExecutor;
    private final ObjectMapper objectMapper;

    @Transactional
    public WorkflowRunResultVO run(Long workflowId, WorkflowRunRequest request) {
        Long tenantId = requireTenantId();
        WorkflowEntity workflow = getWorkflowOrThrow(workflowId, tenantId);
        List<WorkflowNodeEntity> nodes = listNodes(workflowId, tenantId);
        List<WorkflowEdgeEntity> edges = listEdges(workflowId, tenantId);
        if (nodes.isEmpty()) {
            throw new BusinessException("工作流没有可执行的节点");
        }

        Map<String, WorkflowNodeEntity> nodeMap = nodes.stream()
                .collect(Collectors.toMap(WorkflowNodeEntity::getNodeId, node -> node, (a, b) -> a));
        Map<String, List<WorkflowEdgeEntity>> outgoing = edges.stream()
                .collect(Collectors.groupingBy(WorkflowEdgeEntity::getSourceNodeId));

        WorkflowNodeEntity start = nodes.stream()
                .filter(node -> WorkflowNodeType.START.equals(node.getNodeType()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("缺少开始节点"));

        String executionId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime startedAt = LocalDateTime.now();
        long startedMs = System.currentTimeMillis();
        String input = request != null && StringUtils.hasText(request.getInput()) ? request.getInput().trim() : "";
        String currentPayload = input;
        List<WorkflowRunStepVO> steps = new ArrayList<>();

        WorkflowExecutionEntity execution = new WorkflowExecutionEntity();
        execution.setTenantId(tenantId);
        execution.setWorkflowId(workflowId);
        execution.setExecutionId(executionId);
        execution.setStatus(WorkflowExecutionStatus.RUNNING);
        execution.setInputData(writeJson(Map.of("input", input)));
        execution.setStartedAt(startedAt);
        execution.setTriggeredBy(StpUtil.getLoginIdAsLong());
        workflowExecutionMapper.insert(execution);

        String currentNodeId = start.getNodeId();
        int guard = 0;
        Integer finalStatus = WorkflowExecutionStatus.SUCCESS;
        String finalOutput = null;
        String errorMessage = null;

        try {
            while (currentNodeId != null && guard++ < 64) {
                WorkflowNodeEntity node = nodeMap.get(currentNodeId);
                if (node == null) {
                    throw new BusinessException("节点不存在: " + currentNodeId);
                }

                long stepStart = System.currentTimeMillis();
                LocalDateTime stepStartedAt = LocalDateTime.now();
                StepResult stepResult = executeNode(node, currentPayload, tenantId);
                int stepDuration = (int) (System.currentTimeMillis() - stepStart);

                WorkflowNodeLogEntity logEntity = new WorkflowNodeLogEntity();
                logEntity.setTenantId(tenantId);
                logEntity.setExecutionId(executionId);
                logEntity.setNodeId(node.getNodeId());
                logEntity.setNodeType(node.getNodeType());
                logEntity.setStatus(stepResult.success() ? 1 : 2);
                logEntity.setInputData(writeJson(Map.of("input", currentPayload)));
                logEntity.setOutputData(writeJson(Map.of("output", stepResult.output())));
                logEntity.setErrorMessage(stepResult.errorMessage());
                logEntity.setStartedAt(stepStartedAt);
                logEntity.setFinishedAt(LocalDateTime.now());
                logEntity.setDurationMs(stepDuration);
                workflowNodeLogMapper.insert(logEntity);

                steps.add(WorkflowRunStepVO.builder()
                        .nodeId(node.getNodeId())
                        .nodeType(node.getNodeType())
                        .nodeName(node.getNodeName())
                        .status(stepResult.success() ? 1 : 2)
                        .input(currentPayload)
                        .output(stepResult.output())
                        .errorMessage(stepResult.errorMessage())
                        .durationMs(stepDuration)
                        .build());

                if (!stepResult.success()) {
                    finalStatus = WorkflowExecutionStatus.FAILED;
                    errorMessage = stepResult.errorMessage();
                    break;
                }

                currentPayload = stepResult.output();
                if (WorkflowNodeType.END.equals(node.getNodeType())) {
                    finalOutput = currentPayload;
                    break;
                }

                List<WorkflowEdgeEntity> nextEdges = outgoing.getOrDefault(currentNodeId, List.of());
                if (nextEdges.isEmpty()) {
                    if (!WorkflowNodeType.END.equals(node.getNodeType())) {
                        throw new BusinessException("节点「" + node.getNodeName() + "」没有后续连线");
                    }
                    break;
                }
                currentNodeId = nextEdges.get(0).getTargetNodeId();
            }
        } catch (BusinessException e) {
            finalStatus = WorkflowExecutionStatus.FAILED;
            errorMessage = e.getMessage();
        }

        int durationMs = (int) (System.currentTimeMillis() - startedMs);
        execution.setStatus(finalStatus);
        execution.setOutputData(writeJson(Map.of("output", finalOutput != null ? finalOutput : currentPayload)));
        execution.setErrorMessage(errorMessage);
        execution.setFinishedAt(LocalDateTime.now());
        execution.setDurationMs(durationMs);
        workflowExecutionMapper.update(execution);

        return WorkflowRunResultVO.builder()
                .executionId(executionId)
                .status(finalStatus)
                .output(finalOutput != null ? finalOutput : currentPayload)
                .errorMessage(errorMessage)
                .durationMs(durationMs)
                .steps(steps)
                .build();
    }

    private StepResult executeNode(WorkflowNodeEntity node, String input, Long tenantId) {
        return switch (node.getNodeType()) {
            case WorkflowNodeType.START -> StepResult.ok(input);
            case WorkflowNodeType.LLM -> executeLlmNode(node, input, tenantId);
            case WorkflowNodeType.CONDITION -> StepResult.ok(evaluateCondition(node, input));
            case WorkflowNodeType.END -> StepResult.ok(StringUtils.hasText(input) ? input : "流程结束");
            default -> StepResult.fail("暂不支持的节点类型: " + node.getNodeType());
        };
    }

    private StepResult executeLlmNode(WorkflowNodeEntity node, String input, Long tenantId) {
        Map<String, Object> config = parseConfig(node.getNodeConfig());
        Long modelConfigId = toLong(config.get("modelConfigId"));
        String prompt = config.get("prompt") != null ? String.valueOf(config.get("prompt")) : "请处理以下输入：{{input}}";
        String userMessage = prompt.replace("{{input}}", input != null ? input : "");

        try {
            ResolvedModelConfig modelConfig = modelResolutionService.resolve(modelConfigId, tenantId);
            ChatExecuteResult result = chatAgentExecutor.execute(ChatExecuteRequest.builder()
                    .modelConfig(modelConfig)
                    .systemPrompt("你是工作流中的 LLM 节点，请根据指令完成任务。")
                    .userMessage(userMessage)
                    .conversationId("workflow-" + node.getWorkflowId() + "-" + node.getNodeId())
                    .memoryWindow(1)
                    .build());
            return StepResult.ok(result.getReply());
        } catch (Exception e) {
            return StepResult.fail("LLM 节点执行失败: " + rootMessage(e));
        }
    }

    private String evaluateCondition(WorkflowNodeEntity node, String input) {
        Map<String, Object> config = parseConfig(node.getNodeConfig());
        String expression = config.get("expression") != null ? String.valueOf(config.get("expression")) : "";
        if (!StringUtils.hasText(expression)) {
            return input;
        }
        if ("not_empty".equalsIgnoreCase(expression)) {
            return StringUtils.hasText(input) ? "true" : "false";
        }
        if ("contains:success".equalsIgnoreCase(expression)) {
            return input != null && input.toLowerCase().contains("success") ? "true" : "false";
        }
        return input;
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseConfig(String json) {
        if (!StringUtils.hasText(json)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : "未知错误";
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户上下文缺失");
        }
        return tenantId;
    }

    private record StepResult(boolean success, String output, String errorMessage) {
        static StepResult ok(String output) {
            return new StepResult(true, output, null);
        }

        static StepResult fail(String errorMessage) {
            return new StepResult(false, null, errorMessage);
        }
    }
}
