package ai.novaflow.agent.service;

import ai.novaflow.agent.domain.OpenApiAuthContext;
import ai.novaflow.agent.domain.OpenApiCredentialType;
import ai.novaflow.agent.domain.dto.AgentDebugChatRequest;
import ai.novaflow.agent.domain.vo.AgentDebugChatVO;
import ai.novaflow.agent.domain.vo.AgentDebugStreamEvent;
import ai.novaflow.agent.domain.vo.AgentVO;
import ai.novaflow.agent.util.OpenApiCallerIdValidator;
import ai.novaflow.chat.domain.vo.ConversationMessageVO;
import ai.novaflow.chat.domain.vo.ConversationVO;
import ai.novaflow.chat.service.ConversationService;
import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOpenService {

    private static final long SSE_TIMEOUT_MS = 120_000L;

    private final OpenApiAuthService openApiAuthService;
    private final AgentPublishService agentPublishService;
    private final AgentService agentService;
    private final AgentChatService agentChatService;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    public AgentDebugChatVO chat(Long agentId, String rawToken, String callerId, AgentDebugChatRequest request) {
        OpenApiAuthContext auth = openApiAuthService.authenticate(agentId, rawToken);
        String scopedCallerId = requireCallerId(auth, callerId);
        try {
            TenantContext.setTenantId(auth.tenantId());
            agentPublishService.requirePublishedAgent(agentId, auth.tenantId());
            AgentVO agent = agentService.detailWithoutAccessRecord(agentId);
            ensureApiSupported(agent);
            return agentChatService.chat(agent, request, auth.tenantId(), null, "open", scopedCallerId);
        } finally {
            TenantContext.clear();
        }
    }

    public SseEmitter streamChat(Long agentId, String rawToken, String callerId, AgentDebugChatRequest request) {
        OpenApiAuthContext auth = openApiAuthService.authenticate(agentId, rawToken);
        String scopedCallerId = requireCallerId(auth, callerId);
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        CompletableFuture.runAsync(() -> {
            try {
                if (requestAttributes != null) {
                    RequestContextHolder.setRequestAttributes(requestAttributes);
                }
                TenantContext.setTenantId(auth.tenantId());
                agentPublishService.requirePublishedAgent(agentId, auth.tenantId());
                AgentVO agent = agentService.detailWithoutAccessRecord(agentId);
                ensureApiSupported(agent);
                agentChatService.streamChat(agent, request, auth.tenantId(), null, "open", scopedCallerId, emitter);
            } catch (BusinessException e) {
                completeWithError(emitter, e);
            } catch (Exception e) {
                log.error("Open agent stream failed, agentId={}", agentId, e);
                completeWithError(emitter, new BusinessException("模型调用失败: " + rootMessage(e)));
            } finally {
                RequestContextHolder.resetRequestAttributes();
                TenantContext.clear();
            }
        });
        emitter.onTimeout(emitter::complete);
        return emitter;
    }

    public AgentDebugChatVO welcome(Long agentId, String rawToken) {
        OpenApiAuthContext auth = openApiAuthService.authenticate(agentId, rawToken);
        try {
            TenantContext.setTenantId(auth.tenantId());
            agentPublishService.requirePublishedAgent(agentId, auth.tenantId());
            AgentVO agent = agentService.detailWithoutAccessRecord(agentId);
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

    public PageResult<ConversationVO> listConversations(
            Long agentId,
            String rawToken,
            String callerId,
            int page,
            int pageSize) {
        OpenApiAuthContext auth = openApiAuthService.authenticate(agentId, rawToken);
        if (auth.credentialType() == OpenApiCredentialType.EMBED_TOKEN) {
            throw new BusinessException(40303, "Embed Token 无权访问会话列表");
        }
        String scopedCallerId = OpenApiCallerIdValidator.requireValid(callerId);
        try {
            TenantContext.setTenantId(auth.tenantId());
            agentPublishService.requirePublishedAgent(agentId, auth.tenantId());
            return conversationService.pageConversations(
                    agentId, auth.tenantId(), "open", scopedCallerId, page, pageSize);
        } finally {
            TenantContext.clear();
        }
    }

    public List<ConversationMessageVO> listMessages(
            Long agentId,
            String rawToken,
            String callerId,
            String conversationKey) {
        OpenApiAuthContext auth = openApiAuthService.authenticate(agentId, rawToken);
        if (auth.credentialType() == OpenApiCredentialType.EMBED_TOKEN) {
            throw new BusinessException(40303, "Embed Token 无权访问会话消息");
        }
        String scopedCallerId = OpenApiCallerIdValidator.requireValid(callerId);
        try {
            TenantContext.setTenantId(auth.tenantId());
            agentPublishService.requirePublishedAgent(agentId, auth.tenantId());
            return conversationService.listMessages(agentId, auth.tenantId(), conversationKey, scopedCallerId);
        } finally {
            TenantContext.clear();
        }
    }

    private String requireCallerId(OpenApiAuthContext auth, String callerId) {
        return OpenApiCallerIdValidator.requireValid(callerId);
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
