package ai.novaflow.user.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.security.PermissionCodes;
import ai.novaflow.user.domain.dto.RoleSaveRequest;
import ai.novaflow.user.entity.PermissionEntity;
import ai.novaflow.user.entity.RoleEntity;
import ai.novaflow.user.mapper.PermissionMapper;
import ai.novaflow.user.mapper.RoleMapper;
import ai.novaflow.user.mapper.RolePermissionMapper;
import ai.novaflow.tenant.mapper.TenantMemberMapper;
import ai.novaflow.user.mapper.UserMapper;
import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleManagementServiceTest {

    @Mock
    private RoleMapper roleMapper;
    @Mock
    private PermissionMapper permissionMapper;
    @Mock
    private RolePermissionMapper rolePermissionMapper;
    @Mock
    private TenantMemberMapper tenantMemberMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private RoleManagementService roleManagementService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(10L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createRoleRejectsForbiddenPermission() {
        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            doNothing().when(permissionService).requireAnyPermission(1L, 10L, PermissionCodes.ROLE_CREATE, PermissionCodes.TENANT_MANAGE);

            RoleSaveRequest request = new RoleSaveRequest();
            request.setRoleName("测试角色");
            request.setPermissionCodes(List.of(PermissionCodes.TENANT_DELETE));

            assertThrows(BusinessException.class, () -> roleManagementService.createRole(request));
        }
    }

    @Test
    void deleteRoleRejectsWhenMembersExist() {
        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            doNothing().when(permissionService).requireAnyPermission(1L, 10L, PermissionCodes.ROLE_DELETE, PermissionCodes.TENANT_MANAGE);

            RoleEntity role = new RoleEntity();
            role.setId(99L);
            role.setTenantId(10L);
            role.setRoleCode("custom_abc");
            role.setIsSystem(0);
            role.setIsDeleted(0);
            when(roleMapper.selectOneByQuery(any())).thenReturn(role);
            when(tenantMemberMapper.selectCountByQuery(any())).thenReturn(2L);

            assertThrows(BusinessException.class, () -> roleManagementService.deleteRole(99L));
        }
    }

    @Test
    void createRolePersistsCustomRole() {
        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            doNothing().when(permissionService).requireAnyPermission(1L, 10L, PermissionCodes.ROLE_CREATE, PermissionCodes.TENANT_MANAGE);
            when(roleMapper.selectCountByQuery(any())).thenReturn(0L);

            PermissionEntity permission = new PermissionEntity();
            permission.setId(1L);
            permission.setPermissionCode(PermissionCodes.AGENT_READ);
            when(permissionMapper.selectListByQuery(any())).thenReturn(List.of(permission));

            RoleSaveRequest request = new RoleSaveRequest();
            request.setRoleName("客服专员");
            request.setDescription("只读客服");
            request.setPermissionCodes(List.of(PermissionCodes.AGENT_READ));

            when(roleMapper.insert(any(RoleEntity.class))).thenAnswer(invocation -> {
                RoleEntity entity = invocation.getArgument(0);
                entity.setId(200L);
                return 1;
            });
            when(permissionService.getPermissionCodesByRoleId(200L)).thenReturn(List.of(PermissionCodes.AGENT_READ));

            var result = roleManagementService.createRole(request);
            assertEquals("客服专员", result.getRoleName());
            verify(rolePermissionMapper).insert(any());
        }
    }
}
