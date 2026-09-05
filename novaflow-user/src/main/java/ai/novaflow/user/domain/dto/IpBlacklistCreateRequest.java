package ai.novaflow.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IpBlacklistCreateRequest {

    @NotBlank(message = "IP 地址不能为空")
    private String ipAddress;

    private String reason;
    private LocalDateTime expireAt;
}
