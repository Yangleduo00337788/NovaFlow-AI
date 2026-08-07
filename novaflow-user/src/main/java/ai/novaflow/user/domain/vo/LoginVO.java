package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginVO {

    private String token;
    private UserInfoVO user;
    private TenantInfoVO tenant;

    @Data
    @Builder
    public static class UserInfoVO {
        private Long id;
        private String username;
        private String nickname;
        private String email;
        private String roleCode;
        private String roleName;
    }

    @Data
    @Builder
    public static class TenantInfoVO {
        private Long id;
        private String tenantName;
        private String planType;
    }
}
