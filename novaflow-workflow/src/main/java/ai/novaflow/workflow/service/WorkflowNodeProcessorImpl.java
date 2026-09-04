package ai.novaflow.workflow.service;

import ai.novaflow.aiengine.agent.ChatAgentExecutor;
import ai.novaflow.aiengine.agent.ChatExecuteRequest;
import ai.novaflow.aiengine.agent.ChatExecuteResult;
import ai.novaflow.model.domain.ResolvedModelConfig;
import ai.novaflow.model.service.ModelResolutionService;
import ai.novaflow.rag.domain.RetrievedChunk;
import ai.novaflow.rag.retrieval.KnowledgeRetrievalService;
import ai.novaflow.tool.domain.HttpToolDefinition;
import ai.novaflow.tool.executor.ToolExecutorRouter;
import ai.novaflow.tool.service.ToolDefinitionService;
import ai.novaflow.common.workflow.WorkflowAgentInvoker;
import ai.novaflow.common.workflow.WorkflowAgentInvokeResult;
import ai.novaflow.workflow.domain.vo.WorkflowModelUsageVO;
import ai.novaflow.workflow.domain.WorkflowNodeType;
import ai.novaflow.workflowengine.domain.WorkflowExecutionContext;
import ai.novaflow.workflowengine.domain.WorkflowNodeProcessResult;
import ai.novaflow.workflowengine.domain.WorkflowNodeProcessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;

import ai.novaflow.workflowengine.domain.WorkflowNodeDefinition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowNodeProcessorImpl implements WorkflowNodeProcessor {

    private final ModelResolutionService modelResolutionService;
    private final ChatAgentExecutor chatAgentExecutor;
    private final ToolDefinitionService toolDefinitionService;
    private final ToolExecutorRouter toolExecutorRouter;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<WorkflowAgentInvoker> workflowAgentInvoker;

    @Override
    public WorkflowNodeProcessResult process(WorkflowNodeDefinition node, String input, WorkflowExecutionContext context) {
        Long tenantId = context.getTenantId();
        return switch (node.getNodeType()) {
            case WorkflowNodeType.START -> success(input);
            case WorkflowNodeType.LLM -> executeLlmNode(node, input, context);
            case WorkflowNodeType.KNOWLEDGE -> executeKnowledgeNode(node, input, tenantId);
            case WorkflowNodeType.TOOL -> executeToolNode(node, input, tenantId);
            case WorkflowNodeType.AGENT -> executeAgentNode(node, input, context);
            case WorkflowNodeType.CONDITION -> success(evaluateCondition(node, input));
            case WorkflowNodeType.END -> success(StringUtils.hasText(input) ? input : "流程结束");
            default -> failure("暂不支持的节点类型: " + node.getNodeType());
        };
    }

    private WorkflowNodeProcessResult executeAgentNode(
            WorkflowNodeDefinition node,
            String input,
            WorkflowExecutionContext context) {
        WorkflowAgentInvoker invoker = workflowAgentInvoker.getIfAvailable();
        if (invoker == null) {
            return failure("Agent 节点执行器未就绪");
        }
        Map<String, Object> config = parseConfig(node.getNodeConfig());
        Long agentId = toLong(config.get("agentId"));
        String messageTemplate = config.get("messageTemplate") != null
                ? String.valueOf(config.get("messageTemplate"))
                : "{{input}}";
        String message = messageTemplate.replace("{{input}}", input != null ? input : "");
        String conversationKey = "wf-" + context.getExecutionId() + "-" + node.getNodeId();
        WorkflowAgentInvokeResult result = invoker.invoke(
                agentId,
                context.getTenantId(),
                context.getTriggeredByUserId(),
                message,
                conversationKey);
        if (!result.isSuccess()) {
            return failure(result.getErrorMessage());
        }
        return WorkflowNodeProcessResult.builder()
                .success(true)
                .output(result.getOutput())
                .tokensUsed(result.getTokensUsed() != null ? result.getTokensUsed() : 0)
                .build();
    }

    private WorkflowNodeProcessResult executeLlmNode(
            WorkflowNodeDefinition node,
            String input,
            WorkflowExecutionContext context) {
        Long tenantId = context.getTenantId();
        Map<String, Object> config = parseConfig(node.getNodeConfig());
        Long modelConfigId = toLong(config.get("modelConfigId"));
        String prompt = config.get("prompt") != null ? String.valueOf(config.get("prompt")) : "请处理以下输入：{{input}}";
        String userMessage = prompt.replace("{{input}}", input != null ? input : "");
        String executionId = StringUtils.hasText(context.getExecutionId())
                ? context.getExecutionId()
                : "unknown";

        try {
            ResolvedModelConfig modelConfig = modelResolutionService.resolve(modelConfigId, tenantId);
            ChatExecuteResult result = chatAgentExecutor.execute(ChatExecuteRequest.builder()
                    .modelConfig(modelConfig)
                    .systemPrompt("你是工作流中的 LLM 节点，请根据指令完成任务。")
                    .userMessage(userMessage)
                    .conversationId("workflow-" + executionId + "-" + node.getNodeId())
                    .memoryWindow(1)
                    .build());
            int tokensUsed = result.getTokensUsed() != null ? result.getTokensUsed() : 0;
            int inputTokens = result.getInputTokens() != null ? result.getInputTokens() : 0;
            int outputTokens = result.getOutputTokens() != null ? result.getOutputTokens() : 0;
            int total = tokensUsed > 0 ? tokensUsed : inputTokens + outputTokens;
            WorkflowModelUsageVO usage = WorkflowModelUsageVO.builder()
                    .nodeId(node.getNodeId())
                    .nodeName(node.getNodeName())
                    .modelConfigId(modelConfigId)
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .totalTokens(total)
                    .latencyMs(result.getLatencyMs() != null ? result.getLatencyMs().intValue() : null)
                    .build();
            return WorkflowNodeProcessResult.builder()
                    .success(true)
                    .output(result.getReply())
                    .tokensUsed(total)
                    .modelUsage(usage)
                    .build();
        } catch (Exception e) {
            return failure("LLM 节点执行失败: " + rootMessage(e));
        }
    }

    private String evaluateCondition(WorkflowNodeDefinition node, String input) {
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

    private WorkflowNodeProcessResult executeToolNode(WorkflowNodeDefinition node, String input, Long tenantId) {
        Map<String, Object> config = parseConfig(node.getNodeConfig());
        Long toolId = toLong(config.get("toolId"));
        if (toolId == null) {
            return failure("工具节点未选择工具");
        }
        try {
            List<HttpToolDefinition> tools = toolDefinitionService.resolveTools(tenantId, List.of(toolId));
            if (tools.isEmpty()) {
                return failure("工具不存在或未启用");
            }
            HttpToolDefinition tool = tools.get(0);
            if ("skill".equalsIgnoreCase(tool.getToolType())) {
                return failure("Skill 技能不能作为工作流工具节点执行，请使用 MCP 或 HTTP 工具");
            }
            String result = toolExecutorRouter.execute(tool, buildToolArguments(input, config));
            return success(result);
        } catch (Exception e) {
            return failure("工具节点执行失败: " + rootMessage(e));
        }
    }

    private Map<String, Object> buildToolArguments(String input, Map<String, Object> config) {
        Map<String, Object> arguments = new HashMap<>();
        Object configuredArguments = config.get("arguments");
        if (configuredArguments instanceof Map<?, ?> configuredMap) {
            configuredMap.forEach((key, value) -> {
                if (key != null) {
                    arguments.put(String.valueOf(key), value);
                }
            });
        } else if (configuredArguments instanceof String configuredJson
                && StringUtils.hasText(configuredJson.trim())) {
            mergeJsonArguments(arguments, configuredJson.trim());
        }

        if (StringUtils.hasText(input)) {
            String trimmed = input.trim();
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                mergeJsonArguments(arguments, trimmed);
            }
        }

        String fallback = input != null ? input : "";
        arguments.putIfAbsent("input", fallback);
        arguments.putIfAbsent("query", fallback);
        return arguments;
    }

    private void mergeJsonArguments(Map<String, Object> arguments, String json) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            arguments.putAll(parsed);
        } catch (Exception ignored) {
            // 非 JSON 输入时保留默认参数
        }
    }

    private WorkflowNodeProcessResult executeKnowledgeNode(WorkflowNodeDefinition node, String input, Long tenantId) {
        Map<String, Object> config = parseConfig(node.getNodeConfig());
        Long knowledgeBaseId = toLong(config.get("knowledgeBaseId"));
        if (knowledgeBaseId == null) {
            return failure("知识库节点未选择知识库");
        }
        if (!StringUtils.hasText(input)) {
            return failure("知识库检索输入不能为空");
        }
        int topK = toInt(config.get("topK"), 5);
        Float scoreThreshold = toFloat(config.get("scoreThreshold"));
        try {
            List<RetrievedChunk> chunks = knowledgeRetrievalService.retrieve(
                    knowledgeBaseId,
                    tenantId,
                    input.trim(),
                    topK,
                    scoreThreshold
            );
            if (chunks.isEmpty()) {
                return success("未检索到相关内容");
            }
            String output = chunks.stream()
                    .map(RetrievedChunk::getText)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining("\n\n---\n\n"));
            return success(StringUtils.hasText(output) ? output : "未检索到相关内容");
        } catch (Exception e) {
            return failure("知识库节点执行失败: " + rootMessage(e));
        }
    }

    private WorkflowNodeProcessResult success(String output) {
        return WorkflowNodeProcessResult.builder()
                .success(true)
                .output(output)
                .build();
    }

    private WorkflowNodeProcessResult failure(String errorMessage) {
        return WorkflowNodeProcessResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
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

    private int toInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Float toFloat(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.floatValue();
        }
        try {
            return Float.parseFloat(String.valueOf(value));
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
}
