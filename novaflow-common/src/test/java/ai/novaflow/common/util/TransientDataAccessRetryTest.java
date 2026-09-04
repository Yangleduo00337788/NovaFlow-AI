package ai.novaflow.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.QueryTimeoutException;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransientDataAccessRetryTest {

    @Test
    void treatsDeadlockAndDuplicateAsRetryable() {
        assertTrue(TransientDataAccessRetry.isRetryable(new DuplicateKeyException("dup")));
        assertTrue(TransientDataAccessRetry.isRetryable(new DataIntegrityViolationException("uk")));
        assertTrue(TransientDataAccessRetry.isRetryable(new CannotAcquireLockException("lock")));
        assertTrue(TransientDataAccessRetry.isRetryable(new QueryTimeoutException("timeout")));
        assertTrue(TransientDataAccessRetry.isRetryable(new RuntimeException(new SQLException("Deadlock found", "40001"))));
        assertFalse(TransientDataAccessRetry.isRetryable(new IllegalStateException("no")));
    }

    @Test
    void retriesUntilSuccess() {
        AtomicInteger calls = new AtomicInteger();
        String result = TransientDataAccessRetry.execute(5, () -> {
            if (calls.incrementAndGet() < 3) {
                throw new DuplicateKeyException("dup");
            }
            return "ok";
        });
        assertEquals("ok", result);
        assertEquals(3, calls.get());
    }

    @Test
    void givesUpAfterMaxAttempts() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(DuplicateKeyException.class, () ->
                TransientDataAccessRetry.execute(3, () -> {
                    calls.incrementAndGet();
                    throw new DuplicateKeyException("dup");
                }));
        assertEquals(3, calls.get());
    }
}
