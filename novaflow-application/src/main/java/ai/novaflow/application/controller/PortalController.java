package ai.novaflow.application.controller;
import ai.novaflow.common.security.PermissionCodes;

import ai.novaflow.application.domain.dto.PortalFavoriteToggleRequest;
import ai.novaflow.application.domain.vo.PortalAppDetailVO;
import ai.novaflow.application.domain.vo.PortalAppVO;
import ai.novaflow.application.domain.vo.PortalBrandingVO;
import ai.novaflow.application.domain.vo.PortalCategoryVO;
import ai.novaflow.application.service.PortalService;
import ai.novaflow.chat.domain.vo.ConversationMessageVO;
import ai.novaflow.chat.domain.vo.ConversationVO;
import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.domain.PageResult;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/portal")
@RequiredArgsConstructor
public class PortalController {

    private final PortalService portalService;

    @SaCheckPermission(PermissionCodes.PORTAL_ACCESS)
    @GetMapping("/branding")
    public ApiResult<PortalBrandingVO> branding() {
        return ApiResult.ok(portalService.getBranding());
    }

    @SaCheckPermission(PermissionCodes.PORTAL_ACCESS)
    @GetMapping("/categories")
    public ApiResult<List<PortalCategoryVO>> categories() {
        return ApiResult.ok(portalService.listCategories());
    }

    @SaCheckPermission(PermissionCodes.PORTAL_ACCESS)
    @GetMapping("/apps")
    public ApiResult<List<PortalAppVO>> listApps(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean favoritesOnly) {
        return ApiResult.ok(portalService.listPublishedApps(category, favoritesOnly));
    }

    @SaCheckPermission(PermissionCodes.PORTAL_ACCESS)
    @GetMapping("/apps/{id}")
    public ApiResult<PortalAppDetailVO> appDetail(@PathVariable Long id) {
        return ApiResult.ok(portalService.getPublishedApp(id));
    }

    @SaCheckPermission(PermissionCodes.PORTAL_ACCESS)
    @PostMapping("/favorites/toggle")
    public ApiResult<Boolean> toggleFavorite(@Valid @RequestBody PortalFavoriteToggleRequest request) {
        return ApiResult.ok(portalService.toggleFavorite(request.getApplicationId()));
    }

    @SaCheckPermission(PermissionCodes.PORTAL_ACCESS)
    @GetMapping("/apps/{id}/conversations")
    public ApiResult<PageResult<ConversationVO>> listConversations(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int pageSize) {
        return ApiResult.ok(portalService.listMyConversations(id, page, pageSize));
    }

    @SaCheckPermission(PermissionCodes.PORTAL_ACCESS)
    @GetMapping("/apps/{id}/conversations/messages")
    public ApiResult<List<ConversationMessageVO>> listConversationMessages(
            @PathVariable Long id,
            @RequestParam String conversationKey) {
        return ApiResult.ok(portalService.listMyMessages(id, conversationKey));
    }

    @SaCheckPermission(PermissionCodes.PORTAL_ACCESS)
    @GetMapping("/apps/{id}/conversations/export")
    public ResponseEntity<byte[]> exportConversation(
            @PathVariable Long id,
            @RequestParam String conversationKey,
            @RequestParam(defaultValue = "markdown") String format) {
        String content = portalService.exportConversation(id, conversationKey, format);
        boolean json = "json".equalsIgnoreCase(format);
        String filename = "portal-conversation-" + id + (json ? ".json" : ".md");
        MediaType mediaType = json ? MediaType.APPLICATION_JSON : MediaType.TEXT_PLAIN;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType(mediaType, StandardCharsets.UTF_8))
                .body(content.getBytes(StandardCharsets.UTF_8));
    }
}
