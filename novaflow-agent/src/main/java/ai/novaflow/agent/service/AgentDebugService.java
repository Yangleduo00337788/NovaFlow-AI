package ai.novaflow.agent.service;

import ai.novaflow.agent.domain.dto.AgentDebugChatRequest;
import ai.novaflow.agent.domain.vo.AgentDebugChatVO;
import ai.novaflow.agent.domain.vo.AgentVO;
import ai.novaflow.aiengine.agent.ChatAgentExecutor;
import ai.novaflow.aiengine.agent.ChatExecuteRequest;
import ai.novaflow.aiengine.agent.ChatExecuteResult;
import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.model.domain.ResolvedModelConfig;
import ai.novaflow.model.domain.dto.ModelUsageRecordRequest;
import ai.novaflow.model.service.ModelResolutionService;
import ai.novaflow.model.service.ModelUsageService;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentDebugService {

    private final AgentService agentService;
    private final ModelResolutionService modelResolutionService;
    private final ModelUsageService modelUsageService;
    private final ChatAgentExecutor chatAgentExecutor;

    public AgentDebugChatVO chat(Long agentId, AgentDebugChatRequest request) {
        AgentVO agent = agentService.detail(agentId);
        String message = request.getMessage().trim();

        if (!"chat".equals(agent.getAgentType())) {
            return buildMockResponse(agent, message);
        }

        Long tenantId = requireTenantId();
        ResolvedModelConfig modelConfig = modelResolutionService.resolve(
                agent.getModelConfigId(),
                tenantId,
                agent.getTemperature(),
                agent.getMaxTokens()
        );

        String conversationId = StringUtils.hasText(request.getConversationId())
                ? request.getConversationId()
                : "debug-" + agentId;

        try {
            ChatExecuteResult result = chatAgentExecutor.execute(ChatExecuteRequest.builder()
                    .modelConfig(modelConfig)
                    .systemPrompt(agent.getSystemPrompt())
                    .userMessage(message)
                    .conversationId(conversationId)
                    .memoryWindow(agent.getMemoryWindow())
                    .build());

            modelUsageService.record(ModelUsageRecordRequest.builder()
                    .tenantId(tenantId)
                    .applicationId(agent.getApplicationId())
                    .agentId(agentId)
                    .userId(StpUtil.getLoginIdAsLong())
                    .modelConfigId(modelConfig.getModelConfigId())
                    .usageType("chat")
                    .inputTokens(result.getInputTokens())
                    .outputTokens(result.getOutputTokens())
                    .totalTokens(result.getTokensUsed())
                    .latencyMs(result.getLatencyMs())
                    .build());

            return AgentDebugChatVO.builder()
                    .reply(result.getReply())
                    .agentName(agent.getAgentName())
                    .tokensUsed(result.getTokensUsed())
                    .latencyMs(result.getLatencyMs())
                    .debugMode(false)
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Agent debug chat failed, agentId={}", agentId, e);
            throw new BusinessException("模型调用失败: " + rootMessage(e));
        }
    }

    public AgentDebugChatVO welcome(Long agentId) {
        AgentVO agent = agentService.detail(agentId);
        String welcome = StringUtils.hasText(agent.getWelcomeMessage())
                ? agent.getWelcomeMessage()
                : "您好，我是 " + agent.getAgentName() + "，有什么可以帮您？";

        return AgentDebugChatVO.builder()
                .reply(welcome)
                .agentName(agent.getAgentName())
                .tokensUsed(0)
                .latencyMs(0L)
                .debugMode(!"chat".equals(agent.getAgentType()))
                .build();
    }

    public void clearConversation(Long agentId, String conversationId) {
        if (StringUtils.hasText(conversationId)) {
            chatAgentExecutor.clearConversation(conversationId);
        }
    }

    private AgentDebugChatVO buildMockResponse(AgentVO agent, String message) {
        long start = System.currentTimeMillis();
        String reply = buildMockReply(agent, message);
        int tokens = Math.max(1, (message.length() + reply.length()) / 2);

        return AgentDebugChatVO.builder()
                .reply(reply)
                .agentName(agent.getAgentName())
                .tokensUsed(tokens)
                .latencyMs(System.currentTimeMillis() - start + 80)
                .debugMode(true)
                .build();
    }

    private String buildMockReply(AgentVO agent, String message) {
        String promptHint = StringUtils.hasText(agent.getSystemPrompt())
                ? "已加载 System Prompt（" + Math.min(agent.getSystemPrompt().length(), 120) + " 字）"
                : "未配置 System Prompt";

        return switch (agent.getAgentType()) {
            case "rag" -> String.format(
                    "【RAG 调试】%s\n\n您的问题：%s\n\n（调试模式）将检索知识库并基于上下文生成回答。%s",
                    agent.getAgentName(), message, promptHint);
            case "tool" -> String.format(
                    "【Tool 调试】%s\n\n收到指令：%s\n\n（调试模式）将解析意图并调用已绑定工具。%s",
                    agent.getAgentName(), message, promptHint);
            case "workflow" -> String.format(
                    "【Workflow 调试】%s\n\n输入：%s\n\n（调试模式）将按工作流节点逐步执行。%s",
                    agent.getAgentName(), message, promptHint);
            default -> String.format(
                    "【Chat 调试】%s\n\n%s",
                    agent.getAgentName(), message);
        };
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户上下文缺失");
        }
        return tenantId;
    }

    private String rootMessage(Throwable e) {
        Throwable current = e;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : e.getMessage();
    }
}
