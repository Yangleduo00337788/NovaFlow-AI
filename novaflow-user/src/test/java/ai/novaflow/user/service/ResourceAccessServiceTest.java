package ai.novaflow.user.service;

import ai.novaflow.common.security.RoleCodes;
import ai.novaflow.tenant.entity.ResourcePermissionEntity;
import ai.novaflow.tenant.mapper.ResourcePermissionMapper;
import ai.novaflow.user.entity.RoleEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceAccessServiceTest {

    @Mock
    private ResourcePermissionMapper resourcePermissionMapper;
    @Mock
    private PermissionService permissionService;
    @InjectMocks
    private ResourceAccessService resourceAccessService;

    @Test
    void openResourceWhenNoAclConfigured() {
        when(resourcePermissionMapper.selectCountByQuery(any())).thenReturn(0L);
        assertTrue(resourceAccessService.canAccessResource(1L, 10L, "AGENT", 100L, "agent:read"));
    }

    @Test
    void requiresExplicitGrantWhenAclConfigured() {
        when(resourcePermissionMapper.selectCountByQuery(any()))
                .thenReturn(1L)
                .thenReturn(0L);
        RoleEntity developer = new RoleEntity();
        developer.setRoleCode(RoleCodes.DEVELOPER);
        when(permissionService.resolveRole(2L, 10L)).thenReturn(developer);
        assertFalse(resourceAccessService.canAccessResource(2L, 10L, "AGENT", 100L, "agent:read"));
    }
}
