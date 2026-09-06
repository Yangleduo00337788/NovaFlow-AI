package ai.novaflow.knowledge.service;

import ai.novaflow.common.exception.BizErrorCodes;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.knowledge.mapper.DocumentMapper;
import ai.novaflow.tenant.entity.TenantEntity;
import ai.novaflow.tenant.mapper.TenantMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantStorageQuotaServiceTest {

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private TenantMapper tenantMapper;

    @InjectMocks
    private TenantStorageQuotaService tenantStorageQuotaService;

    @Test
    void snapshotMarksQuotaExceeded() {
        TenantEntity tenant = new TenantEntity();
        tenant.setId(1L);
        tenant.setMaxStorageMb(1);
        when(tenantMapper.selectOneById(1L)).thenReturn(tenant);
        when(documentMapper.sumFileSizeByTenant(1L)).thenReturn(1024L * 1024L);

        var usage = tenantStorageQuotaService.snapshot(1L);

        assertTrue(usage.isQuotaExceeded());
        assertEquals(100, usage.getUsedPercent());
    }

    @Test
    void assertCanUploadRejectsWhenExceedingLimit() {
        TenantEntity tenant = new TenantEntity();
        tenant.setId(2L);
        tenant.setMaxStorageMb(1);
        when(tenantMapper.selectOneById(2L)).thenReturn(tenant);
        when(documentMapper.sumFileSizeByTenant(2L)).thenReturn(900L * 1024L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                tenantStorageQuotaService.assertCanUpload(2L, 200L * 1024L));
        assertEquals(BizErrorCodes.STORAGE_QUOTA_EXCEEDED, ex.getCode());
    }

    @Test
    void assertCanUploadAllowsWhenWithinLimit() {
        TenantEntity tenant = new TenantEntity();
        tenant.setId(3L);
        tenant.setMaxStorageMb(10);
        when(tenantMapper.selectOneById(3L)).thenReturn(tenant);
        when(documentMapper.sumFileSizeByTenant(3L)).thenReturn(1024L);

        tenantStorageQuotaService.assertCanUpload(3L, 1024L);

        var usage = tenantStorageQuotaService.snapshot(3L);
        assertFalse(usage.isQuotaExceeded());
    }
}
