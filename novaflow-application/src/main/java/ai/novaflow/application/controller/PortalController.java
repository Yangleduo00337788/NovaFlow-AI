package ai.novaflow.application.controller;

import ai.novaflow.application.domain.vo.PortalAppDetailVO;
import ai.novaflow.application.domain.vo.PortalAppVO;
import ai.novaflow.application.service.PortalService;
import ai.novaflow.chat.domain.vo.ConversationMessageVO;
import ai.novaflow.chat.domain.vo.ConversationVO;
import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.domain.PageResult;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portal")
@RequiredArgsConstructor
public class PortalController {

    private final PortalService portalService;

    @SaCheckPermission("portal:access")
    @GetMapping("/apps")
    public ApiResult<List<PortalAppVO>> listApps() {
        return ApiResult.ok(portalService.listPublishedApps());
    }

    @SaCheckPermission("portal:access")
    @GetMapping("/apps/{id}")
    public ApiResult<PortalAppDetailVO> appDetail(@PathVariable Long id) {
        return ApiResult.ok(portalService.getPublishedApp(id));
    }

    @SaCheckPermission("portal:access")
    @GetMapping("/apps/{id}/conversations")
    public ApiResult<PageResult<ConversationVO>> listConversations(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int pageSize) {
        return ApiResult.ok(portalService.listMyConversations(id, page, pageSize));
    }

    @SaCheckPermission("portal:access")
    @GetMapping("/apps/{id}/conversations/messages")
    public ApiResult<List<ConversationMessageVO>> listConversationMessages(
            @PathVariable Long id,
            @RequestParam String conversationKey) {
        return ApiResult.ok(portalService.listMyMessages(id, conversationKey));
    }
}
