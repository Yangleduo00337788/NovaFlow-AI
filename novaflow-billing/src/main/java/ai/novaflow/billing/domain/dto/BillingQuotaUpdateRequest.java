package ai.novaflow.billing.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BillingQuotaUpdateRequest {

    @NotNull(message = "月度 Token 配额不能为空")
    @Min(value = 1, message = "月度 Token 配额至少为 1")
    @Max(value = 9_999_999_999_999L, message = "月度 Token 配额过大")
    private Long monthlyTokenQuota;
}
