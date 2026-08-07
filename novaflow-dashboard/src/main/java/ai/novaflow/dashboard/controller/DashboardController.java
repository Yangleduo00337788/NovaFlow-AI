package ai.novaflow.dashboard.controller;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.dashboard.domain.DashboardOverviewVO;
import ai.novaflow.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public ApiResult<DashboardOverviewVO> overview() {
        return ApiResult.ok(dashboardService.getOverview());
    }
}
