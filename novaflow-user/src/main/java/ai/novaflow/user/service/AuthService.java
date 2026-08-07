package ai.novaflow.user.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.user.domain.dto.LoginRequest;
import ai.novaflow.user.domain.vo.LoginVO;
import ai.novaflow.user.entity.RoleEntity;
import ai.novaflow.user.entity.TenantEntity;
import ai.novaflow.user.entity.TenantMemberEntity;
import ai.novaflow.user.entity.UserEntity;
import ai.novaflow.user.mapper.RoleMapper;
import ai.novaflow.user.mapper.TenantMapper;
import ai.novaflow.user.mapper.TenantMemberMapper;
import ai.novaflow.user.mapper.UserMapper;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final TenantMapper tenantMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    public LoginVO login(LoginRequest request, HttpServletRequest httpRequest) {
        UserEntity user = userMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("email", request.getEmail())
                        .eq("is_deleted", 0)
        );
        if (user == null || user.getStatus() != 1) {
            throw new BusinessException("账号不存在或已禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("邮箱或密码错误");
        }

        TenantMemberEntity member = tenantMemberMapper.selectOneByQuery(
                QueryWrapper.create().where("user_id = ?", user.getId()).and("is_deleted = 0").limit(1)
        );
        if (member == null) {
            throw new BusinessException("用户未加入任何企业");
        }

        TenantEntity tenant = tenantMapper.selectOneById(member.getTenantId());
        RoleEntity role = roleMapper.selectOneById(member.getRoleId());

        StpUtil.login(user.getId());
        StpUtil.getSession().set("tenantId", tenant.getId());
        StpUtil.getSession().set("roleCode", role != null ? role.getRoleCode() : "user");

        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(httpRequest.getRemoteAddr());
        userMapper.update(user);

        TenantContext.setTenantId(tenant.getId());

        return LoginVO.builder()
                .token(StpUtil.getTokenValue())
                .user(LoginVO.UserInfoVO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .email(user.getEmail())
                        .roleCode(role != null ? role.getRoleCode() : null)
                        .roleName(role != null ? role.getRoleName() : null)
                        .build())
                .tenant(LoginVO.TenantInfoVO.builder()
                        .id(tenant.getId())
                        .tenantName(tenant.getTenantName())
                        .planType(tenant.getPlanType())
                        .build())
                .build();
    }

    public LoginVO currentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        Long tenantId = (Long) StpUtil.getSession().get("tenantId");

        UserEntity user = userMapper.selectOneById(userId);
        TenantEntity tenant = tenantMapper.selectOneById(tenantId);
        TenantMemberEntity member = tenantMemberMapper.selectOneByQuery(
                QueryWrapper.create().where("user_id = ?", userId).and("tenant_id = ?", tenantId).limit(1)
        );
        RoleEntity role = member != null ? roleMapper.selectOneById(member.getRoleId()) : null;

        return LoginVO.builder()
                .token(StpUtil.getTokenValue())
                .user(LoginVO.UserInfoVO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .email(user.getEmail())
                        .roleCode(role != null ? role.getRoleCode() : null)
                        .roleName(role != null ? role.getRoleName() : null)
                        .build())
                .tenant(LoginVO.TenantInfoVO.builder()
                        .id(tenant.getId())
                        .tenantName(tenant.getTenantName())
                        .planType(tenant.getPlanType())
                        .build())
                .build();
    }

    public void logout() {
        StpUtil.logout();
    }
}
