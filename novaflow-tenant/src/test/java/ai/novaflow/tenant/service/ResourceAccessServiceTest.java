package ai.novaflow.tenant.service;

import ai.novaflow.common.security.PermissionCodes;
import ai.novaflow.common.security.ResourceAclBypassChecker;
import ai.novaflow.tenant.entity.ResourcePermissionEntity;
import ai.novaflow.tenant.mapper.ResourcePermissionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceAccessServiceTest {

    @Mock
    private ResourcePermissionMapper resourcePermissionMapper;
    @Mock
    private ResourceAclBypassChecker resourceAclBypassChecker;
    @InjectMocks
    private ResourceAccessService resourceAccessService;

    @Test
    void openResourceWhenNoAclConfigured() {
        when(resourcePermissionMapper.selectCountByQuery(any())).thenReturn(0L);
        assertTrue(resourceAccessService.canAccessResource(1L, 10L, "AGENT", 100L, PermissionCodes.AGENT_READ));
    }

    @Test
    void requiresExplicitGrantWhenAclConfigured() {
        when(resourcePermissionMapper.selectCountByQuery(any()))
                .thenReturn(1L)
                .thenReturn(0L);
        when(resourceAclBypassChecker.bypassesResourceAcl(2L, 10L)).thenReturn(false);
        assertFalse(resourceAccessService.canAccessResource(2L, 10L, "AGENT", 100L, PermissionCodes.AGENT_READ));
    }

    @Test
    void listAccessibleResourceIdsUsesBatchQueries() {
        ResourcePermissionEntity aclRow = new ResourcePermissionEntity();
        aclRow.setResourceId(100L);
        ResourcePermissionEntity grantRow = new ResourcePermissionEntity();
        grantRow.setResourceId(100L);

        when(resourcePermissionMapper.selectListByQuery(any()))
                .thenReturn(List.of(aclRow))
                .thenReturn(List.of(grantRow));
        when(resourceAclBypassChecker.bypassesResourceAcl(2L, 10L)).thenReturn(false);

        Set<Long> accessible = resourceAccessService.listAccessibleResourceIds(
                2L, 10L, "AGENT", PermissionCodes.AGENT_READ, List.of(100L, 200L));

        assertEquals(Set.of(100L, 200L), accessible);
        verify(resourcePermissionMapper, org.mockito.Mockito.times(2)).selectListByQuery(any());
    }
}
