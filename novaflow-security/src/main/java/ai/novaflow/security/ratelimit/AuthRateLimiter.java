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
public class AuthRateLimiter {

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${novaflow.auth.login-rate-limit-per-minute:20}")
    private int loginRateLimitPerMinute;

    public void checkLogin(String email, String clientIp) {
        check("login", email, clientIp, loginRateLimitPerMinute);
    }

    public void checkRegister(String email, String clientIp) {
        check("register", email, clientIp, loginRateLimitPerMinute);
    }

    private void check(String action, String email, String clientIp, int limitPerMinute) {
        String fingerprint = SecureUtil.sha256(
                action + "|" + safe(email).toLowerCase() + "|" + safeIp(clientIp)
        );
        String key = "novaflow:auth:" + action + ":rate:" + fingerprint;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, Duration.ofMinutes(1));
        }
        if (count != null && count > limitPerMinute) {
            throw new BusinessException(42901, "请求过于频繁，请稍后再试");
        }
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "unknown";
    }

    private String safeIp(String clientIp) {
        return StringUtils.hasText(clientIp) ? clientIp.trim() : "unknown";
    }
}
