package ai.novaflow.security.session;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SessionTenantIdsTest {

    @Test
    void convertsCommonSessionTypes() {
        assertEquals(7L, SessionTenantIds.toLong(7L));
        assertEquals(7L, SessionTenantIds.toLong(7));
        assertEquals(7L, SessionTenantIds.toLong(new BigDecimal("7")));
        assertEquals(7L, SessionTenantIds.toLong("7"));
        assertNull(SessionTenantIds.toLong(null));
        assertNull(SessionTenantIds.toLong("x"));
    }
}
