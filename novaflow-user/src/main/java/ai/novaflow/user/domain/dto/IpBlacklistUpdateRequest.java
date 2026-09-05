package ai.novaflow.user.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IpBlacklistUpdateRequest {

    private String reason;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private LocalDateTime expireAt;
}
