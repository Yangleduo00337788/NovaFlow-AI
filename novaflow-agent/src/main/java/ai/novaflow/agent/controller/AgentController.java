package ai.novaflow.agent.controller;

import ai.novaflow.agent.domain.dto.AgentDebugChatRequest;
import ai.novaflow.agent.domain.dto.AgentSaveRequest;
import ai.novaflow.agent.domain.vo.AgentDebugChatVO;
import ai.novaflow.agent.domain.vo.AgentPublishVO;
import ai.novaflow.agent.domain.vo.AgentVO;
import ai.novaflow.chat.domain.vo.ConversationMessageVO;
import ai.novaflow.chat.domain.vo.ConversationVO;
import ai.novaflow.agent.domain.vo.DebugAttachmentVO;
import ai.novaflow.agent.service.AgentDebugAttachmentService;
import ai.novaflow.agent.service.AgentDebugService;
import ai.novaflow.agent.service.AgentPublishService;
import ai.novaflow.agent.service.AgentService;
import ai.novaflow.chat.service.ConversationService;
import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.security.PermissionCodes;
import ai.novaflow.common.domain.PageResult;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final AgentDebugService agentDebugService;
    private final AgentDebugAttachmentService agentDebugAttachmentService;
    private final AgentPublishService agentPublishService;
    private final ConversationService conversationService;

    @SaCheckPermission(value = {PermissionCodes.AGENT_READ, PermissionCodes.AGENT_CREATE, PermissionCodes.AGENT_EDIT}, mode = SaMode.OR)
    @GetMapping
    public ApiResult<PageResult<AgentVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String agentType) {
        return ApiResult.ok(agentService.page(page, pageSize, keyword, agentType));
    }

    @SaCheckPermission(value = {PermissionCodes.AGENT_READ, PermissionCodes.AGENT_CREATE, PermissionCodes.AGENT_EDIT}, mode = SaMode.OR)
    @GetMapping("/{id}")
    public ApiResult<AgentVO> detail(@PathVariable Long id) {
        return ApiResult.ok(agentService.detail(id));
    }

    @SaCheckPermission(PermissionCodes.AGENT_CREATE)
    @PostMapping
    public ApiResult<AgentVO> create(@Valid @RequestBody AgentSaveRequest request) {
        return ApiResult.ok(agentService.create(request));
    }

    @SaCheckPermission(PermissionCodes.AGENT_EDIT)
    @PutMapping("/{id}")
    public ApiResult<AgentVO> update(@PathVariable Long id, @Valid @RequestBody AgentSaveRequest request) {
        return ApiResult.ok(agentService.update(id, request));
    }

    @SaCheckPermission(PermissionCodes.AGENT_DELETE)
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        agentService.delete(id);
        return ApiResult.ok();
    }

    @SaCheckPermission(value = {PermissionCodes.AGENT_PUBLISH, PermissionCodes.AGENT_EDIT, PermissionCodes.API_READ}, mode = SaMode.OR)
    @GetMapping("/{id}/publish")
    public ApiResult<AgentPublishVO> publishInfo(@PathVariable Long id) {
        return ApiResult.ok(agentPublishService.getPublishInfo(id));
    }
    @SaCheckPermission(PermissionCodes.AGENT_PUBLISH)
    @PostMapping("/{id}/publish")
    public ApiResult<AgentPublishVO> publish(@PathVariable Long id) {
        return ApiResult.ok(agentPublishService.publish(id));
    }

    @SaCheckPermission(PermissionCodes.AGENT_PUBLISH)
    @PostMapping("/{id}/unpublish")
    public ApiResult<AgentPublishVO> unpublish(@PathVariable Long id) {
        return ApiResult.ok(agentPublishService.unpublish(id));
    }

    @SaCheckPermission(value = {PermissionCodes.AGENT_PUBLISH, PermissionCodes.API_CREATE, PermissionCodes.API_UPDATE}, mode = SaMode.OR)
    @PostMapping("/{id}/rotate-api-key")
    public ApiResult<AgentPublishVO> rotateApiKey(@PathVariable Long id) {
        return ApiResult.ok(agentPublishService.rotateApiKey(id));
    }

    @SaCheckPermission(PermissionCodes.AGENT_PUBLISH)
    @PostMapping("/{id}/rotate-embed-token")
    public ApiResult<AgentPublishVO> rotateEmbedToken(@PathVariable Long id) {
        return ApiResult.ok(agentPublishService.rotateEmbedToken(id));
    }

    @SaCheckPermission(value = {
            PermissionCodes.AGENT_DEBUG,
            PermissionCodes.AGENT_EDIT,
            PermissionCodes.AGENT_CHAT,
            PermissionCodes.PORTAL_ACCESS
    }, mode = SaMode.OR)
    @GetMapping("/{id}/debug/welcome")
    public ApiResult<AgentDebugChatVO> debugWelcome(@PathVariable Long id) {
        return ApiResult.ok(agentDebugService.welcome(id));
    }

    @SaCheckPermission(value = {
            PermissionCodes.AGENT_DEBUG,
            PermissionCodes.AGENT_EDIT,
            PermissionCodes.AGENT_CHAT,
            PermissionCodes.PORTAL_ACCESS
    }, mode = SaMode.OR)
    @PostMapping("/{id}/debug/chat")
    public ApiResult<AgentDebugChatVO> debugChat(
            @PathVariable Long id,
            @Valid @RequestBody AgentDebugChatRequest request) {
        return ApiResult.ok(agentDebugService.chat(id, request));
    }

    @SaCheckPermission(value = { PermissionCodes.AGENT_DEBUG, PermissionCodes.AGENT_EDIT }, mode = SaMode.OR)
    @PostMapping("/{id}/debug/attachments")
    public ApiResult<DebugAttachmentVO> uploadDebugAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        agentService.getAgentEntityOrThrow(id);
        return ApiResult.ok(agentDebugAttachmentService.parse(file));
    }

    @SaCheckPermission(value = {
            PermissionCodes.AGENT_DEBUG,
            PermissionCodes.AGENT_EDIT,
            PermissionCodes.AGENT_CHAT,
            PermissionCodes.PORTAL_ACCESS
    }, mode = SaMode.OR)
    @PostMapping(value = "/{id}/debug/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter debugChatStream(
            @PathVariable Long id,
            @Valid @RequestBody AgentDebugChatRequest request) {
        return agentDebugService.streamChat(id, request);
    }

    @SaCheckPermission(value = { PermissionCodes.AGENT_DEBUG, PermissionCodes.AGENT_EDIT }, mode = SaMode.OR)
    @DeleteMapping("/{id}/debug/conversation")
    public ApiResult<Void> clearDebugConversation(
            @PathVariable Long id,
            @RequestParam String conversationId) {
        agentService.getAgentEntityOrThrow(id);
        agentDebugService.clearConversation(id, conversationId);
        return ApiResult.ok();
    }

    @SaCheckPermission(value = { PermissionCodes.AGENT_DEBUG, PermissionCodes.AGENT_EDIT }, mode = SaMode.OR)
    @GetMapping("/{id}/debug/conversations")
    public ApiResult<PageResult<ConversationVO>> listDebugConversations(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        agentService.getAgentEntityOrThrow(id);
        return ApiResult.ok(conversationService.pageConversations(
                id, TenantContext.getTenantId(), "debug", null, StpUtil.getLoginIdAsLong(), null, page, pageSize));
    }

    @SaCheckPermission(value = { PermissionCodes.AGENT_DEBUG, PermissionCodes.AGENT_EDIT }, mode = SaMode.OR)
    @GetMapping("/{id}/debug/conversations/messages")
    public ApiResult<List<ConversationMessageVO>> listDebugConversationMessages(
            @PathVariable Long id,
            @RequestParam String conversationKey) {
        agentService.getAgentEntityOrThrow(id);
        return ApiResult.ok(conversationService.listMessages(
                id, TenantContext.getTenantId(), conversationKey, null, StpUtil.getLoginIdAsLong()));
    }
}
