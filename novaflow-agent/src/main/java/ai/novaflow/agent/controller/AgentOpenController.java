package ai.novaflow.agent.controller;

import ai.novaflow.agent.domain.dto.AgentDebugChatRequest;
import ai.novaflow.agent.domain.vo.AgentDebugChatVO;
import ai.novaflow.chat.domain.vo.ConversationMessageVO;
import ai.novaflow.chat.domain.vo.ConversationVO;
import ai.novaflow.agent.service.AgentOpenService;
import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.security.ratelimit.OpenApiRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/open/agents")
@RequiredArgsConstructor
public class AgentOpenController {

    private static final String HEADER_CALLER_ID = "X-Caller-Id";

    private final AgentOpenService agentOpenService;
    private final OpenApiRateLimiter openApiRateLimiter;

    @GetMapping("/{id}/welcome")
    public ApiResult<AgentDebugChatVO> welcome(@PathVariable Long id, HttpServletRequest request) {
        OpenApiRequestContext ctx = resolveRequestContext(request);
        return ApiResult.ok(agentOpenService.welcome(id, ctx.token()));
    }

    @PostMapping("/{id}/chat")
    public ApiResult<AgentDebugChatVO> chat(
            @PathVariable Long id,
            @Valid @RequestBody AgentDebugChatRequest request,
            HttpServletRequest httpRequest) {
        OpenApiRequestContext ctx = resolveRequestContext(httpRequest);
        return ApiResult.ok(agentOpenService.chat(id, ctx.token(), ctx.callerId(), request));
    }

    @PostMapping(value = "/{id}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @PathVariable Long id,
            @Valid @RequestBody AgentDebugChatRequest request,
            HttpServletRequest httpRequest) {
        OpenApiRequestContext ctx = resolveRequestContext(httpRequest);
        return agentOpenService.streamChat(id, ctx.token(), ctx.callerId(), request);
    }

    @GetMapping("/{id}/conversations")
    public ApiResult<PageResult<ConversationVO>> listConversations(
            @PathVariable Long id,
            HttpServletRequest request,
            @RequestParam String callerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        OpenApiRequestContext ctx = resolveRequestContext(request);
        return ApiResult.ok(agentOpenService.listConversations(id, ctx.token(), callerId, page, pageSize));
    }

    @GetMapping("/{id}/conversations/messages")
    public ApiResult<List<ConversationMessageVO>> listConversationMessages(
            @PathVariable Long id,
            @RequestParam String conversationKey,
            @RequestParam String callerId,
            HttpServletRequest request) {
        OpenApiRequestContext ctx = resolveRequestContext(request);
        return ApiResult.ok(agentOpenService.listMessages(id, ctx.token(), callerId, conversationKey));
    }

    private OpenApiRequestContext resolveRequestContext(HttpServletRequest request) {
        String token = resolveToken(request);
        openApiRateLimiter.check(token, request.getRemoteAddr());
        String callerId = request.getHeader(HEADER_CALLER_ID);
        return new OpenApiRequestContext(token, callerId);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        String token = null;
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7).trim();
        }
        if (!StringUtils.hasText(token)) {
            String headerKey = request.getHeader("X-API-Key");
            if (StringUtils.hasText(headerKey)) {
                token = headerKey.trim();
            }
        }
        if (!StringUtils.hasText(token)) {
            String embedToken = request.getHeader("X-Embed-Token");
            if (StringUtils.hasText(embedToken)) {
                token = embedToken.trim();
            }
        }
        return token;
    }

    private record OpenApiRequestContext(String token, String callerId) {
    }
}
