package ai.novaflow.dashboard.controller;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.dashboard.domain.DashboardOverviewVO;
import ai.novaflow.dashboard.domain.dto.FavoriteToggleRequest;
import ai.novaflow.dashboard.service.DashboardService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@SaCheckPermission(value = {
        "dashboard:view",
        "agent:create", "agent:edit", "agent:read",
        "monitor:view", "application:read", "application:manage", "tenant:manage"
}, mode = SaMode.OR)
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public ApiResult<DashboardOverviewVO> overview() {
        return ApiResult.ok(dashboardService.getOverview());
    }

    @GetMapping("/recent-items")
    public ApiResult<List<DashboardOverviewVO.RecentItemVO>> recentItems(
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResult.ok(dashboardService.listRecentItems(limit));
    }

    @GetMapping("/favorites")
    public ApiResult<List<DashboardOverviewVO.RecentItemVO>> favorites(
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResult.ok(dashboardService.listFavoriteItems(limit));
    }

    @GetMapping("/published-workflows")
    public ApiResult<List<DashboardOverviewVO.PublishedWorkflowVO>> publishedWorkflows(
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResult.ok(dashboardService.listPublishedWorkflows(limit));
    }

    @PostMapping("/favorites/toggle")
    public ApiResult<Boolean> toggleFavorite(@RequestBody FavoriteToggleRequest request) {
        return ApiResult.ok(dashboardService.toggleFavorite(request));
    }

    @GetMapping("/workflows/{workflowId}/runtime")
    public ApiResult<DashboardOverviewVO.WorkflowRuntimeVO> workflowRuntime(@PathVariable Long workflowId) {
        return ApiResult.ok(dashboardService.getWorkflowRuntime(workflowId));
    }
}
