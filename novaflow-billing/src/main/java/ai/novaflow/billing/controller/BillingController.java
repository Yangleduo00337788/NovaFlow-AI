package ai.novaflow.billing.controller;

import ai.novaflow.billing.domain.vo.BillingOverviewVO;
import ai.novaflow.billing.domain.vo.BillingQuotaVO;
import ai.novaflow.billing.service.BillingService;
import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.model.domain.vo.TokenUsageLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/overview")
    public ApiResult<BillingOverviewVO> overview(@RequestParam(required = false) String month) {
        return ApiResult.ok(billingService.getOverview(month));
    }

    @GetMapping("/quota")
    public ApiResult<BillingQuotaVO> quota() {
        return ApiResult.ok(billingService.getQuota());
    }

    @GetMapping("/records")
    public ApiResult<PageResult<TokenUsageLogVO>> records(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) String usageType,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(billingService.pageRecords(page, pageSize, agentId, usageType, month, keyword));
    }
}
