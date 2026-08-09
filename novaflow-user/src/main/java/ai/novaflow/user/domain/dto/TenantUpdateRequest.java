package ai.novaflow.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TenantUpdateRequest {

    @NotBlank(message = "企业名称不能为空")
    @Size(max = 128, message = "企业名称不能超过 128 个字符")
    private String tenantName;

    private String logoUrl;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
}
