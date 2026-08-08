package ai.novaflow.agent.controller;

import ai.novaflow.agent.domain.dto.AgentDebugChatRequest;
import ai.novaflow.agent.domain.vo.AgentDebugChatVO;
import ai.novaflow.agent.service.AgentOpenService;
import ai.novaflow.common.domain.ApiResult;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/open/agents")
@RequiredArgsConstructor
public class AgentOpenController {

    private final AgentOpenService agentOpenService;

    @GetMapping("/{id}/welcome")
    public ApiResult<AgentDebugChatVO> welcome(@PathVariable Long id, HttpServletRequest request) {
        return ApiResult.ok(agentOpenService.welcome(id, resolveApiKey(request)));
    }

    @PostMapping("/{id}/chat")
    public ApiResult<AgentDebugChatVO> chat(
            @PathVariable Long id,
            @Valid @RequestBody AgentDebugChatRequest request,
            HttpServletRequest httpRequest) {
        return ApiResult.ok(agentOpenService.chat(id, resolveApiKey(httpRequest), request));
    }

    @PostMapping(value = "/{id}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @PathVariable Long id,
            @Valid @RequestBody AgentDebugChatRequest request,
            HttpServletRequest httpRequest) {
        return agentOpenService.streamChat(id, resolveApiKey(httpRequest), request);
    }

    private String resolveApiKey(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        String apiKey = request.getHeader("X-API-Key");
        if (StringUtils.hasText(apiKey)) {
            return apiKey.trim();
        }
        return null;
    }
}
