package ai.novaflow.monitor.controller;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.monitor.domain.vo.MonitorOverviewVO;
import ai.novaflow.monitor.service.MonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;

    @GetMapping("/overview")
    public ApiResult<MonitorOverviewVO> overview() {
        return ApiResult.ok(monitorService.getOverview());
    }
}
