package ai.novaflow.user.config;

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
        Long tenantId = (Long) StpUtil.getSession().get("tenantId");
        return permissionService.getRoleCodes(Long.parseLong(loginId.toString()), tenantId);
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long tenantId = (Long) StpUtil.getSession().get("tenantId");
        return permissionService.getPermissionCodes(Long.parseLong(loginId.toString()), tenantId);
    }
}
