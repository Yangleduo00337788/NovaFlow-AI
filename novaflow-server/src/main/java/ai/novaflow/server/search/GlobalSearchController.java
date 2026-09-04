package ai.novaflow.server.search;
import ai.novaflow.common.security.PermissionCodes;

import ai.novaflow.common.domain.ApiResult;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;

    @SaCheckPermission(PermissionCodes.SEARCH_GLOBAL)
    @GetMapping
    public ApiResult<List<GlobalSearchItemVO>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResult.ok(globalSearchService.search(keyword, limit));
    }
}
