package ai.novaflow.agent.service;

import ai.novaflow.agent.domain.dto.AgentDebugChatRequest;
import ai.novaflow.agent.domain.vo.AgentDebugChatVO;
import ai.novaflow.agent.domain.vo.AgentDebugStreamEvent;
import ai.novaflow.agent.domain.vo.AgentVO;
import ai.novaflow.agent.domain.vo.ModelCapabilitiesVO;
import ai.novaflow.chat.service.ConversationService;
import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.model.domain.ModelCapabilityResolver;
import ai.novaflow.model.domain.ResolvedModelConfig;
import ai.novaflow.model.service.ModelResolutionService;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentDebugService {

    private static final long SSE_TIMEOUT_MS = 120_000L;

    private final AgentService agentService;
    private final AgentChatService agentChatService;
    private final ConversationService conversationService;
    private final ModelResolutionService modelResolutionService;
    private final ObjectMapper objectMapper;

    public AgentDebugChatVO chat(Long agentId, AgentDebugChatRequest request) {
        AgentVO agent = agentService.detailWithoutAccessRecord(agentId);
        String message = request.getMessage().trim();

        if (!agentChatService.supportsRealExecution(agent)) {
            AgentDebugChatVO mock = buildMockResponse(agent, message);
            persistMockExchange(agent.getId(), requireTenantId(), StpUtil.getLoginIdAsLong(), request, mock);
            return mock;
        }

        try {
            return agentChatService.chat(
                    agent,
                    request,
                    requireTenantId(),
                    StpUtil.getLoginIdAsLong(),
                    "debug");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Agent debug chat failed, agentId={}", agentId, e);
            throw new BusinessException("模型调用失败: " + rootMessage(e));
        }
    }

    public SseEmitter streamChat(Long agentId, AgentDebugChatRequest request) {
        AgentVO agent = agentService.detailWithoutAccessRecord(agentId);
        String message = request.getMessage().trim();
        Long userId = StpUtil.getLoginIdAsLong();
        Long tenantId = requireTenantId();
        boolean realExecution = agentChatService.supportsRealExecution(agent);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        CompletableFuture.runAsync(() -> {
            try {
                TenantContext.setTenantId(tenantId);
                if (!realExecution) {
                    streamMockResponse(emitter, agent, request, tenantId, userId);
                    return;
                }
                agentChatService.streamChat(agent, request, tenantId, userId, "debug", emitter);
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
        AgentVO agent = agentService.detailWithoutAccessRecord(agentId);
        String welcome = StringUtils.hasText(agent.getWelcomeMessage())
                ? agent.getWelcomeMessage()
                : "您好，我是 " + agent.getAgentName() + "，有什么可以帮您？";

        ModelCapabilitiesVO capabilities = null;
        String modelName = null;
        String providerName = null;
        if (agentChatService.supportsRealExecution(agent)) {
            try {
                ResolvedModelConfig modelConfig = modelResolutionService.resolve(
                        agent.getModelConfigId(),
                        requireTenantId(),
                        agent.getTemperature(),
                        agent.getMaxTokens()
                );
                modelName = modelConfig.getModelName();
                providerName = modelConfig.getProviderName();
                capabilities = ModelCapabilitiesVO.builder()
                        .supportsDeepThinking(ModelCapabilityResolver.supportsDeepThinking(
                                modelConfig.getProviderCode(), modelConfig.getModelName()))
                        .supportsWebSearch(ModelCapabilityResolver.supportsWebSearch(
                                modelConfig.getProviderCode(), modelConfig.getModelName()))
                        .build();
            } catch (BusinessException ignored) {
                capabilities = ModelCapabilitiesVO.builder()
                        .supportsDeepThinking(false)
                        .supportsWebSearch(false)
                        .build();
            }
        }

        return AgentDebugChatVO.builder()
                .reply(welcome)
                .agentName(agent.getAgentName())
                .tokensUsed(0)
                .latencyMs(0L)
                .debugMode(!agentChatService.supportsRealExecution(agent))
                .modelCapabilities(capabilities)
                .modelName(modelName)
                .providerName(providerName)
                .build();
    }

    public void clearConversation(Long agentId, String conversationId) {
        agentChatService.clearConversation(conversationId);
    }

    private void streamMockResponse(
            SseEmitter emitter,
            AgentVO agent,
            AgentDebugChatRequest request,
            Long tenantId,
            Long userId) throws IOException {
        long start = System.currentTimeMillis();
        AgentDebugChatVO mock = buildMockResponse(agent, request.getMessage().trim());
        for (char ch : mock.getReply().toCharArray()) {
            sendEvent(emitter, AgentDebugStreamEvent.builder()
                    .type("token")
                    .content(String.valueOf(ch))
                    .build());
        }
        mock.setLatencyMs(System.currentTimeMillis() - start);
        persistMockExchange(agent.getId(), tenantId, userId, request, mock);
        sendEvent(emitter, AgentDebugStreamEvent.builder()
                .type("done")
                .reply(mock.getReply())
                .agentName(mock.getAgentName())
                .tokensUsed(mock.getTokensUsed())
                .latencyMs(mock.getLatencyMs())
                .debugMode(true)
                .build());
        emitter.complete();
    }

    private void persistMockExchange(
            Long agentId,
            Long tenantId,
            Long userId,
            AgentDebugChatRequest request,
            AgentDebugChatVO response) {
        if (!StringUtils.hasText(request.getConversationId()) || !StringUtils.hasText(request.getMessage())) {
            return;
        }
        try {
            conversationService.persistExchange(ConversationService.ExchangeRequest.builder()
                    .tenantId(tenantId)
                    .agentId(agentId)
                    .conversationKey(request.getConversationId())
                    .channel("debug")
                    .userId(userId)
                    .userMessage(request.getMessage().trim())
                    .assistantReply(response.getReply())
                    .tokensUsed(response.getTokensUsed())
                    .latencyMs(response.getLatencyMs())
                    .sources(List.of())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to persist mock conversation, agentId={}", agentId, e);
        }
    }

    private void sendEvent(SseEmitter emitter, AgentDebugStreamEvent event) {
        try {
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(event)));
        } catch (Exception e) {
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
