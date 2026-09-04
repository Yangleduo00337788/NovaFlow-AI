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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenApiRateLimiterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private OpenApiRateLimiter openApiRateLimiter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(openApiRateLimiter, "rateLimitPerMinute", 60);
        ReflectionTestUtils.setField(openApiRateLimiter, "ipRateLimitPerMinute", 2);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void throws42901WhenIpExceedsLimit() {
        when(valueOperations.increment(anyString())).thenReturn(3L);
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> openApiRateLimiter.check("nf_test_key", "10.0.0.1")
        );
        assertEquals(42901, exception.getCode());
        assertEquals("请求过于频繁，请稍后再试", exception.getMessage());
    }
}
