package ai.novaflow.user.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.tenant.entity.ResourcePermissionEntity;
import ai.novaflow.tenant.mapper.ResourcePermissionMapper;
import ai.novaflow.user.domain.dto.ResourcePermissionSaveRequest;
import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class ResourcePermissionAdminServiceTest {

    @Mock
    private ResourcePermissionMapper resourcePermissionMapper;
    @Mock
    private ResourceAccessService resourceAccessService;
    @InjectMocks
    private ResourcePermissionAdminService resourcePermissionAdminService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(10L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void replaceReactivatesSoftDeletedGrantInsteadOfInserting() {
        ResourcePermissionEntity deleted = new ResourcePermissionEntity();
        deleted.setId(5L);
        deleted.setTenantId(10L);
        deleted.setResourceType("AGENT");
        deleted.setResourceId(100L);
        deleted.setUserId(2L);
        deleted.setPermissionCode("agent:read");
        deleted.setIsDeleted(1);

        when(resourcePermissionMapper.selectListByQuery(any())).thenReturn(List.of(deleted));

        ResourcePermissionSaveRequest request = new ResourcePermissionSaveRequest();
        ResourcePermissionSaveRequest.GrantItem grant = new ResourcePermissionSaveRequest.GrantItem();
        grant.setUserId(2L);
        grant.setPermissionCode("agent:read");
        request.setGrants(List.of(grant));

        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            List<ResourcePermissionEntity> saved = resourcePermissionAdminService.replace("AGENT", 100L, request);
            assertEquals(1, saved.size());
            assertEquals(5L, saved.get(0).getId());
        }

        verify(resourcePermissionMapper, never()).insert(any());
        ArgumentCaptor<ResourcePermissionEntity> captor = ArgumentCaptor.forClass(ResourcePermissionEntity.class);
        verify(resourcePermissionMapper, times(1)).update(captor.capture());
        assertEquals(0, captor.getValue().getIsDeleted());
    }
}
