package ai.novaflow.billing.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BillingAlertSaveRequest {

    private Long id;

    @NotBlank(message = "预警名称不能为空")
    private String alertName;

    @NotNull(message = "阈值不能为空")
    @Min(1)
    @Max(100)
    private Integer thresholdPercent;

    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;

    private List<String> notifyChannels;
}
