package ai.novaflow.agent.util;

import ai.novaflow.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenApiCallerIdValidatorTest {

    @Test
    void acceptsValidCallerId() {
        String callerId = OpenApiCallerIdValidator.requireValid("user-abc-12345");
        assertEquals("user-abc-12345", callerId);
    }

    @Test
    void rejectsBlankCallerId() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> OpenApiCallerIdValidator.requireValid("  "));
        assertEquals(40001, ex.getCode());
    }

    @Test
    void rejectsTooShortCallerId() {
        assertThrows(BusinessException.class, () -> OpenApiCallerIdValidator.requireValid("abc"));
    }
}
