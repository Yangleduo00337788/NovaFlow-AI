package ai.novaflow.application.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PortalFavoriteToggleRequest {

    @NotNull(message = "应用 ID 不能为空")
    private Long applicationId;
}
