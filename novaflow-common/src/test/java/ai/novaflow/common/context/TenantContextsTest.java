package ai.novaflow.common.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantContextsTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void requireTenantIdReturnsBoundTenant() {
        TenantContext.setTenantId(42L);
        assertEquals(42L, TenantContexts.requireTenantId());
    }

    @Test
    void requireTenantIdFailsWhenMissing() {
        assertThrows(RuntimeException.class, TenantContexts::requireTenantId);
    }
}
