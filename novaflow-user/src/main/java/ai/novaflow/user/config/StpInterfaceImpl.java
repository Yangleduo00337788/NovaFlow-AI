package ai.novaflow.user.config;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.security.session.SessionTenantIds;
import ai.novaflow.user.service.PermissionService;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final PermissionService permissionService;

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return permissionService.getRoleCodes(Long.parseLong(loginId.toString()), currentTenantId());
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return permissionService.getPermissionCodes(Long.parseLong(loginId.toString()), currentTenantId());
    }

    private Long currentTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        return SessionTenantIds.toLong(StpUtil.getSession().get("tenantId"));
    }
}
