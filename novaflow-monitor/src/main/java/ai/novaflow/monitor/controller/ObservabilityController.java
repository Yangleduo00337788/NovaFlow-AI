package ai.novaflow.monitor.controller;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.monitor.domain.vo.ObservabilityOverviewVO;
import ai.novaflow.monitor.service.TraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitor")
@RequiredArgsConstructor
public class ObservabilityController {

    private final TraceService traceService;

    @GetMapping("/observability")
    public ApiResult<ObservabilityOverviewVO> observability() {
        return ApiResult.ok(traceService.getObservabilityOverview());
    }
}
