package ai.novaflow.user.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlatformModelProviderUpdateRequest {

    @NotNull(message = "启用状态不能为空")
    private Integer enabled;
}
