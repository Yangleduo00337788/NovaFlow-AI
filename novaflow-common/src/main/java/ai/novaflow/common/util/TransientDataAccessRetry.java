package ai.novaflow.common.util;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.TransientDataAccessException;

import java.sql.SQLException;
import java.util.function.Supplier;

public final class TransientDataAccessRetry {

    public static final int DEFAULT_MAX_ATTEMPTS = 5;

    private TransientDataAccessRetry() {
    }

    public static boolean isRetryable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof DeadlockLoserDataAccessException
                    || current instanceof CannotAcquireLockException
                    || current instanceof DataIntegrityViolationException
                    || current instanceof TransientDataAccessException) {
                return true;
            }
            if (current instanceof SQLException sql) {
                String state = sql.getSQLState();
                if ("40001".equals(state) || "41000".equals(state)) {
                    return true;
                }
                String message = sql.getMessage();
                if (message != null && message.toLowerCase().contains("deadlock")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    public static <T> T execute(int maxAttempts, Supplier<T> action) {
        DataAccessException last = null;
        int attempts = Math.max(maxAttempts, 1);
        for (int i = 1; i <= attempts; i++) {
            try {
                return action.get();
            } catch (DataAccessException e) {
                last = e;
                if (!isRetryable(e) || i == attempts) {
                    throw e;
                }
                sleep(15L * i);
            }
        }
        throw last;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
