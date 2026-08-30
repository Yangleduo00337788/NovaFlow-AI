package ai.novaflow.security.ratelimit;

import ai.novaflow.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginFailureLockServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private LoginFailureLockService loginFailureLockService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loginFailureLockService, "failureThreshold", 3);
        ReflectionTestUtils.setField(loginFailureLockService, "lockMinutes", 15);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void checkLockedPassesWhenNotLocked() {
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        assertDoesNotThrow(() -> loginFailureLockService.checkLocked("user@example.com", "127.0.0.1"));
    }

    @Test
    void checkLockedThrowsWhenLocked() {
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> loginFailureLockService.checkLocked("user@example.com", "127.0.0.1")
        );
        assertEquals(42902, exception.getCode());
    }

    @Test
    void recordFailureLocksAfterThreshold() {
        when(valueOperations.increment(anyString())).thenReturn(1L, 2L, 3L);

        loginFailureLockService.recordFailure("user@example.com", "127.0.0.1");
        loginFailureLockService.recordFailure("user@example.com", "127.0.0.1");
        loginFailureLockService.recordFailure("user@example.com", "127.0.0.1");

        verify(stringRedisTemplate).expire(anyString(), eq(Duration.ofMinutes(15)));
        verify(valueOperations).set(anyString(), eq("3"), eq(Duration.ofMinutes(15)));
        verify(stringRedisTemplate).delete(anyString());
    }

    @Test
    void clearFailuresRemovesCounters() {
        loginFailureLockService.clearFailures("user@example.com", "127.0.0.1");
        verify(stringRedisTemplate, times(2)).delete(anyString());
        verify(valueOperations, never()).increment(anyString());
    }
}
