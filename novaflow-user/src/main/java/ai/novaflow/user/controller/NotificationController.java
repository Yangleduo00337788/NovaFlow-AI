package ai.novaflow.user.controller;
import ai.novaflow.common.context.TenantContexts;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.user.domain.vo.UserNotificationVO;
import ai.novaflow.user.service.NotificationService;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResult<PageResult<UserNotificationVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResult.ok(notificationService.page(TenantContexts.requireTenantId(), StpUtil.getLoginIdAsLong(), page, pageSize));
    }

    @GetMapping("/unread-count")
    public ApiResult<Long> unreadCount() {
        return ApiResult.ok(notificationService.unreadCount(TenantContexts.requireTenantId(), StpUtil.getLoginIdAsLong()));
    }

    @PostMapping("/{id}/read")
    public ApiResult<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(TenantContexts.requireTenantId(), StpUtil.getLoginIdAsLong(), id);
        return ApiResult.ok();
    }

    @PostMapping("/read-all")
    public ApiResult<Void> markAllRead() {
        notificationService.markAllRead(TenantContexts.requireTenantId(), StpUtil.getLoginIdAsLong());
        return ApiResult.ok();
    }

}
