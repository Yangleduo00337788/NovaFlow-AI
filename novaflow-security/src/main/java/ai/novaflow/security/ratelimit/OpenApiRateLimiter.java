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
public class OpenApiRateLimiter {

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${novaflow.open-api.rate-limit-per-minute:60}")
    private int rateLimitPerMinute;

    @Value("${novaflow.open-api.ip-rate-limit-per-minute:30}")
    private int ipRateLimitPerMinute;

    public void check(String apiKey, String clientIp) {
        checkIp(clientIp);
        if (!StringUtils.hasText(apiKey)) {
            return;
        }
        String fingerprint = SecureUtil.sha256(apiKey.trim() + "|" + safeIp(clientIp));
        String key = "novaflow:open-api:rate:" + fingerprint;
        incrementAndCheck(key, rateLimitPerMinute);
    }

    private void checkIp(String clientIp) {
        String key = "novaflow:open-api:ip-rate:" + safeIp(clientIp);
        incrementAndCheck(key, ipRateLimitPerMinute);
    }

    private void incrementAndCheck(String key, int limit) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, Duration.ofMinutes(1));
        }
        if (count != null && count > limit) {
            throw new BusinessException(42901, "请求过于频繁，请稍后再试");
        }
    }

    private String safeIp(String clientIp) {
        return StringUtils.hasText(clientIp) ? clientIp.trim() : "unknown";
    }
}
