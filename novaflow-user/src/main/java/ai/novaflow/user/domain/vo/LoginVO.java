package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginVO {

    private String token;
    private UserInfoVO user;
    private TenantInfoVO tenant;
    private List<String> permissions;

    @Data
    @Builder
    public static class UserInfoVO {
        private Long id;
        private String username;
        private String nickname;
        private String email;
        private String accountType;
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
