package ai.novaflow.user.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlatformUserUpdateRequest {

    @NotNull(message = "状态不能为空")
    private Integer status;
}
