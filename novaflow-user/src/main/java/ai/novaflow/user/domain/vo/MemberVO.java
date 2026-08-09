package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MemberVO {

    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String email;
    private String roleCode;
    private String roleName;
    private Integer status;
    private LocalDateTime joinedAt;
    private LocalDateTime lastLoginAt;
}
