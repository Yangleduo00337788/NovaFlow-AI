package ai.novaflow.monitor.controller;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.monitor.domain.vo.TraceDetailVO;
import ai.novaflow.monitor.domain.vo.TraceNodeVO;
import ai.novaflow.monitor.domain.vo.TraceSpanVO;
import ai.novaflow.monitor.service.TraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trace")
@RequiredArgsConstructor
public class TraceController {

    private final TraceService traceService;

    @GetMapping("/spans")
    public ApiResult<PageResult<TraceSpanVO>> pageSpans(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "24h") String timeRange) {
        return ApiResult.ok(traceService.pageSpans(page, pageSize, keyword, type, status, timeRange));
    }

    @GetMapping("/spans/{traceId}")
    public ApiResult<TraceDetailVO> getSpan(@PathVariable String traceId) {
        return ApiResult.ok(traceService.getSpanDetail(traceId));
    }

    @GetMapping("/spans/{traceId}/nodes")
    public ApiResult<List<TraceNodeVO>> listNodes(@PathVariable String traceId) {
        return ApiResult.ok(traceService.listSpanNodes(traceId));
    }
}
