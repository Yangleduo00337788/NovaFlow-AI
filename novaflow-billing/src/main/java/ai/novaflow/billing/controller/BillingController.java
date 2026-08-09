package ai.novaflow.billing.controller;

import ai.novaflow.billing.domain.dto.BillingAlertSaveRequest;
import ai.novaflow.billing.domain.dto.BillingQuotaUpdateRequest;
import ai.novaflow.billing.domain.vo.BillingAlertVO;
import ai.novaflow.billing.domain.vo.BillingOverviewVO;
import ai.novaflow.billing.domain.vo.BillingQuotaVO;
import ai.novaflow.billing.service.BillingAlertService;
import ai.novaflow.billing.service.BillingExportService;
import ai.novaflow.billing.service.BillingService;
import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.model.domain.vo.TokenUsageLogVO;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;
    private final BillingAlertService billingAlertService;
    private final BillingExportService billingExportService;

    @GetMapping("/overview")
    public ApiResult<BillingOverviewVO> overview(@RequestParam(required = false) String month) {
        return ApiResult.ok(billingService.getOverview(month));
    }

    @GetMapping("/quota")
    public ApiResult<BillingQuotaVO> quota() {
        return ApiResult.ok(billingService.getQuota());
    }

    @PutMapping("/quota")
    public ApiResult<BillingQuotaVO> updateQuota(@Valid @RequestBody BillingQuotaUpdateRequest request) {
        return ApiResult.ok(billingService.updateQuota(request));
    }

    @GetMapping("/alerts")
    public ApiResult<List<BillingAlertVO>> alerts() {
        return ApiResult.ok(billingAlertService.listAlerts(requireTenantId()));
    }

    @PutMapping("/alerts")
    public ApiResult<BillingAlertVO> saveAlert(@Valid @RequestBody BillingAlertSaveRequest request) {
        return ApiResult.ok(billingAlertService.saveAlert(
                requireTenantId(),
                StpUtil.getLoginIdAsLong(),
                request));
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

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String month,
            @RequestParam(defaultValue = "excel") String format) throws IOException {
        Long tenantId = requireTenantId();
        String period = month != null ? month.trim() : java.time.YearMonth.now().toString();
        byte[] data;
        String filename;
        MediaType mediaType;
        if ("pdf".equalsIgnoreCase(format)) {
            data = billingExportService.exportPdf(tenantId, period);
            filename = "billing-" + period + ".pdf";
            mediaType = MediaType.APPLICATION_PDF;
        } else {
            data = billingExportService.exportExcel(tenantId, period);
            filename = "billing-" + period + ".xlsx";
            mediaType = MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(mediaType)
                .body(data);
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户上下文缺失");
        }
        return tenantId;
    }
}
