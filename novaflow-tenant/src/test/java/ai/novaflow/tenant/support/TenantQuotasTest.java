package ai.novaflow.tenant.support;

import ai.novaflow.common.exception.BizErrorCodes;
import ai.novaflow.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantQuotasTest {

    @Test
    void storageWithinLimitAllowsUpload() {
        TenantQuotas.assertStorageWithinLimit(100L, 50L, 1024L);
    }

    @Test
    void storageExceededThrowsQuotaError() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                TenantQuotas.assertStorageWithinLimit(900L, 200L, 1024L));
        assertEquals(BizErrorCodes.STORAGE_QUOTA_EXCEEDED, ex.getCode());
    }
}
