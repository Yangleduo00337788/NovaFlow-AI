package ai.novaflow.tool.controller;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.tool.domain.dto.McpServerSaveRequest;
import ai.novaflow.tool.domain.vo.McpServerVO;
import ai.novaflow.tool.service.McpServerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mcp-servers")
@RequiredArgsConstructor
public class McpServerController {

    private final McpServerService mcpServerService;

    @GetMapping
    public ApiResult<PageResult<McpServerVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(mcpServerService.page(page, pageSize, keyword));
    }

    @PostMapping
    public ApiResult<McpServerVO> create(@Valid @RequestBody McpServerSaveRequest request) {
        return ApiResult.ok(mcpServerService.create(request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        mcpServerService.delete(id);
        return ApiResult.ok();
    }
}
