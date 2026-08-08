package ai.novaflow.agent.service;

import ai.novaflow.agent.domain.dto.AgentDebugChatRequest;
import ai.novaflow.agent.domain.vo.AgentDebugChatVO;
import ai.novaflow.agent.domain.vo.AgentDebugStreamEvent;
import ai.novaflow.agent.domain.vo.AgentVO;
import ai.novaflow.agent.entity.AgentApiKeyEntity;
import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOpenService {

    private static final long SSE_TIMEOUT_MS = 120_000L;

    private final AgentApiKeyService agentApiKeyService;
    private final AgentPublishService agentPublishService;
    private final AgentService agentService;
    private final AgentChatService agentChatService;
    private final ObjectMapper objectMapper;

    public AgentDebugChatVO chat(Long agentId, String rawApiKey, AgentDebugChatRequest request) {
        AgentApiKeyEntity apiKey = agentApiKeyService.authenticate(agentId, rawApiKey);
        try {
            TenantContext.setTenantId(apiKey.getTenantId());
            agentPublishService.requirePublishedAgent(agentId, apiKey.getTenantId());
            AgentVO agent = agentService.detail(agentId);
            ensureApiSupported(agent);
            return agentChatService.chat(agent, request, apiKey.getTenantId(), null, "open");
        } finally {
            TenantContext.clear();
        }
    }

    public SseEmitter streamChat(Long agentId, String rawApiKey, AgentDebugChatRequest request) {
        AgentApiKeyEntity apiKey = agentApiKeyService.authenticate(agentId, rawApiKey);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        CompletableFuture.runAsync(() -> {
            try {
                TenantContext.setTenantId(apiKey.getTenantId());
                agentPublishService.requirePublishedAgent(agentId, apiKey.getTenantId());
                AgentVO agent = agentService.detail(agentId);
                ensureApiSupported(agent);
                agentChatService.streamChat(agent, request, apiKey.getTenantId(), null, "open", emitter);
            } catch (BusinessException e) {
                completeWithError(emitter, e);
            } catch (Exception e) {
                log.error("Open agent stream failed, agentId={}", agentId, e);
                completeWithError(emitter, new BusinessException("模型调用失败: " + rootMessage(e)));
            } finally {
                TenantContext.clear();
            }
        });
        emitter.onTimeout(emitter::complete);
        return emitter;
    }

    public AgentDebugChatVO welcome(Long agentId, String rawApiKey) {
        AgentApiKeyEntity apiKey = agentApiKeyService.authenticate(agentId, rawApiKey);
        try {
            TenantContext.setTenantId(apiKey.getTenantId());
            agentPublishService.requirePublishedAgent(agentId, apiKey.getTenantId());
            AgentVO agent = agentService.detail(agentId);
            String welcome = StringUtils.hasText(agent.getWelcomeMessage())
                    ? agent.getWelcomeMessage()
                    : "您好，我是 " + agent.getAgentName() + "，有什么可以帮您？";
            return AgentDebugChatVO.builder()
                    .reply(welcome)
                    .agentName(agent.getAgentName())
                    .tokensUsed(0)
                    .latencyMs(0L)
                    .debugMode(false)
                    .build();
        } finally {
            TenantContext.clear();
        }
    }

    private void ensureApiSupported(AgentVO agent) {
        if (!agentChatService.supportsRealExecution(agent)) {
            throw new BusinessException("该 Agent 类型暂不支持对外 API");
        }
    }

    private void completeWithError(SseEmitter emitter, Throwable error) {
        try {
            String message = error instanceof BusinessException businessException
                    ? businessException.getMessage()
                    : rootMessage(error);
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(
                    AgentDebugStreamEvent.builder().type("error").message(message).build())));
            emitter.complete();
        } catch (Exception ex) {
            emitter.completeWithError(error);
        }
    }

    private String rootMessage(Throwable e) {
        Throwable current = e;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : e.getMessage();
    }
}
