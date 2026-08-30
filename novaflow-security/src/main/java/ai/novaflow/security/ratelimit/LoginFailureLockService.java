package ai.novaflow.security.ratelimit;

import ai.novaflow.common.exception.BusinessException;
import cn.hutool.crypto.SecureUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class LoginFailureLockService {

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${novaflow.auth.login-failure-threshold:5}")
    private int failureThreshold;

    @Value("${novaflow.auth.login-lock-minutes:15}")
    private int lockMinutes;

    public void checkLocked(String email, String clientIp) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey(email, clientIp)))) {
            throw new BusinessException(42902, "登录失败次数过多，请 " + lockMinutes + " 分钟后再试");
        }
    }

    public void recordFailure(String email, String clientIp) {
        String failKey = failKey(email, clientIp);
        Long count = stringRedisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(failKey, Duration.ofMinutes(lockMinutes));
        }
        if (count != null && count >= failureThreshold) {
            stringRedisTemplate.opsForValue().set(
                    lockKey(email, clientIp),
                    String.valueOf(count),
                    Duration.ofMinutes(lockMinutes)
            );
            stringRedisTemplate.delete(failKey);
        }
    }

    public void clearFailures(String email, String clientIp) {
        stringRedisTemplate.delete(failKey(email, clientIp));
        stringRedisTemplate.delete(lockKey(email, clientIp));
    }

    private String failKey(String email, String clientIp) {
        return "novaflow:auth:login:fail:" + fingerprint(email, clientIp);
    }

    private String lockKey(String email, String clientIp) {
        return "novaflow:auth:login:lock:" + fingerprint(email, clientIp);
    }

    private String fingerprint(String email, String clientIp) {
        return SecureUtil.sha256(safe(email).toLowerCase() + "|" + safeIp(clientIp));
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "unknown";
    }

    private String safeIp(String clientIp) {
        return StringUtils.hasText(clientIp) ? clientIp.trim() : "unknown";
    }
}
