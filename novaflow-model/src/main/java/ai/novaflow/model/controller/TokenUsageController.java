package ai.novaflow.model.controller;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.model.domain.vo.TokenUsageLogVO;
import ai.novaflow.model.service.TokenUsageLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/token-usage")
@RequiredArgsConstructor
public class TokenUsageController {

    private final TokenUsageLogService tokenUsageLogService;

    @GetMapping("/logs")
    public ApiResult<PageResult<TokenUsageLogVO>> pageLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) String usageType) {
        return ApiResult.ok(tokenUsageLogService.page(page, pageSize, agentId, keyword, success, usageType));
    }

    @GetMapping("/logs/export")
    public ResponseEntity<byte[]> exportLogs(
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) String usageType) {
        byte[] data = tokenUsageLogService.exportCsv(agentId, keyword, success, usageType);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=token-usage-logs.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(data);
    }
}
