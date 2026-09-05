package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PlatformUserVO {

    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String accountType;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private LocalDateTime createdAt;
    private List<PlatformUserMembershipVO> memberships;
}
