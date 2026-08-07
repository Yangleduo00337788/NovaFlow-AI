package ai.novaflow.agent.service;

import ai.novaflow.agent.domain.dto.AgentDebugChatRequest;
import ai.novaflow.agent.domain.vo.AgentDebugChatVO;
import ai.novaflow.agent.domain.vo.AgentDebugStreamEvent;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentDebugService {

    private static final long SSE_TIMEOUT_MS = 120_000L;

    private final AgentService agentService;
    private final ModelResolutionService modelResolutionService;
    private final ModelUsageService modelUsageService;
    private final ChatAgentExecutor chatAgentExecutor;
    private final ObjectMapper objectMapper;

    public AgentDebugChatVO chat(Long agentId, AgentDebugChatRequest request) {
        AgentVO agent = agentService.detail(agentId);
        String message = request.getMessage().trim();

        if (!"chat".equals(agent.getAgentType())) {
            return buildMockResponse(agent, message);
        }

        DebugChatContext context = buildChatContext(agentId, agent, request, StpUtil.getLoginIdAsLong());
        try {
            ChatExecuteResult result = chatAgentExecutor.execute(context.toExecuteRequest(message));
            recordUsage(context, result);
            return toChatVO(agent, result, false);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Agent debug chat failed, agentId={}", agentId, e);
            throw new BusinessException("模型调用失败: " + rootMessage(e));
        }
    }

    public SseEmitter streamChat(Long agentId, AgentDebugChatRequest request) {
        AgentVO agent = agentService.detail(agentId);
        String message = request.getMessage().trim();
        Long userId = StpUtil.getLoginIdAsLong();
        DebugChatContext context = null;
        ChatExecuteRequest executeRequest = null;
        if ("chat".equals(agent.getAgentType())) {
            context = buildChatContext(agentId, agent, request, userId);
            executeRequest = context.toExecuteRequest(message);
        }
        final DebugChatContext chatContext = context;
        final ChatExecuteRequest chatExecuteRequest = executeRequest;

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        CompletableFuture.runAsync(() -> {
            Long tenantId = chatContext != null ? chatContext.tenantId() : null;
            try {
                if (tenantId != null) {
                    TenantContext.setTenantId(tenantId);
                }
                if (!"chat".equals(agent.getAgentType())) {
                    streamMockResponse(emitter, agent, message);
                    return;
                }

                chatAgentExecutor.executeStream(chatExecuteRequest, new ai.novaflow.aiengine.agent.ChatStreamListener() {
                    @Override
                    public void onToken(String token) {
                        sendEvent(emitter, AgentDebugStreamEvent.builder()
                                .type("token")
                                .content(token)
                                .build());
                    }

                    @Override
                    public void onComplete(ChatExecuteResult result) {
                        recordUsage(chatContext, result);
                        sendEvent(emitter, AgentDebugStreamEvent.builder()
                                .type("done")
                                .reply(result.getReply())
                                .agentName(agent.getAgentName())
                                .tokensUsed(result.getTokensUsed())
                                .latencyMs(result.getLatencyMs())
                                .debugMode(false)
                                .build());
                        emitter.complete();
                    }

                    @Override
                    public void onError(Throwable error) {
                        completeWithError(emitter, error);
                    }
                });
            } catch (BusinessException e) {
                completeWithError(emitter, e);
            } catch (Exception e) {
                log.error("Agent debug stream failed, agentId={}", agentId, e);
                completeWithError(emitter, new BusinessException("模型调用失败: " + rootMessage(e)));
            } finally {
                TenantContext.clear();
            }
        });

        emitter.onTimeout(emitter::complete);
        return emitter;
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

    private DebugChatContext buildChatContext(Long agentId, AgentVO agent, AgentDebugChatRequest request, Long userId) {
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
        return new DebugChatContext(agentId, agent, tenantId, userId, modelConfig, conversationId, agent.getMemoryWindow());
    }

    private void recordUsage(DebugChatContext context, ChatExecuteResult result) {
        modelUsageService.record(ModelUsageRecordRequest.builder()
                .tenantId(context.tenantId())
                .applicationId(context.agent().getApplicationId())
                .agentId(context.agentId())
                .userId(context.userId())
                .modelConfigId(context.modelConfig().getModelConfigId())
                .usageType("chat")
                .inputTokens(result.getInputTokens())
                .outputTokens(result.getOutputTokens())
                .totalTokens(result.getTokensUsed())
                .latencyMs(result.getLatencyMs())
                .build());
    }

    private AgentDebugChatVO toChatVO(AgentVO agent, ChatExecuteResult result, boolean debugMode) {
        return AgentDebugChatVO.builder()
                .reply(result.getReply())
                .agentName(agent.getAgentName())
                .tokensUsed(result.getTokensUsed())
                .latencyMs(result.getLatencyMs())
                .debugMode(debugMode)
                .build();
    }

    private void streamMockResponse(SseEmitter emitter, AgentVO agent, String message) throws IOException {
        long start = System.currentTimeMillis();
        AgentDebugChatVO mock = buildMockResponse(agent, message);
        for (char ch : mock.getReply().toCharArray()) {
            sendEvent(emitter, AgentDebugStreamEvent.builder()
                    .type("token")
                    .content(String.valueOf(ch))
                    .build());
        }
        sendEvent(emitter, AgentDebugStreamEvent.builder()
                .type("done")
                .reply(mock.getReply())
                .agentName(mock.getAgentName())
                .tokensUsed(mock.getTokensUsed())
                .latencyMs(System.currentTimeMillis() - start)
                .debugMode(true)
                .build());
        emitter.complete();
    }

    private void sendEvent(SseEmitter emitter, AgentDebugStreamEvent event) {
        try {
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException e) {
            throw new BusinessException("流式响应序列化失败");
        } catch (IOException e) {
            throw new BusinessException("流式响应发送失败");
        }
    }

    private void completeWithError(SseEmitter emitter, Throwable error) {
        try {
            String message = error instanceof BusinessException businessException
                    ? businessException.getMessage()
                    : rootMessage(error);
            sendEvent(emitter, AgentDebugStreamEvent.builder()
                    .type("error")
                    .message(message)
                    .build());
            emitter.complete();
        } catch (Exception ex) {
            emitter.completeWithError(error);
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

    private record DebugChatContext(
            Long agentId,
            AgentVO agent,
            Long tenantId,
            Long userId,
            ResolvedModelConfig modelConfig,
            String conversationId,
            Integer memoryWindow
    ) {
        ChatExecuteRequest toExecuteRequest(String userMessage) {
            return ChatExecuteRequest.builder()
                    .modelConfig(modelConfig)
                    .systemPrompt(agent.getSystemPrompt())
                    .userMessage(userMessage)
                    .conversationId(conversationId)
                    .memoryWindow(memoryWindow)
                    .build();
        }
    }
}
