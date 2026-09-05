package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IpBlacklistVO {

    private Long id;
    private String ipAddress;
    private String reason;
    private Integer status;
    private LocalDateTime expireAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
