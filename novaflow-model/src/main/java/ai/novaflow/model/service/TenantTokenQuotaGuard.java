package ai.novaflow.model.service;

import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.model.mapper.TokenUsageMapper;
import ai.novaflow.tenant.entity.TenantEntity;
import ai.novaflow.tenant.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的月度 Token 配额原子预占，缓解并发下 TOCTOU 超额问题。
 */
@Service
@RequiredArgsConstructor
public class TenantTokenQuotaGuard {

    private static final long DEFAULT_RESERVE = 1L;
    private static final String KEY_PREFIX = "novaflow:quota:monthly:";

    private final StringRedisTemplate stringRedisTemplate;
    private final TokenUsageMapper tokenUsageMapper;
    private final TenantMapper tenantMapper;

    public void checkAndReserve(Long tenantId) {
        checkAndReserve(tenantId, DEFAULT_RESERVE);
    }

    public void checkAndReserve(Long tenantId, long reserveTokens) {
        if (tenantId == null) {
            return;
        }
        long reserve = Math.max(reserveTokens, 1L);
        TenantEntity tenant = tenantMapper.selectOneById(tenantId);
        if (tenant == null || tenant.getMonthlyTokenQuota() == null || tenant.getMonthlyTokenQuota() <= 0) {
            return;
        }
        long quota = tenant.getMonthlyTokenQuota();
        String key = monthlyKey(tenantId);
        ensureInitialized(key, tenantId);
        Long usedAfterReserve = stringRedisTemplate.opsForValue().increment(key, reserve);
        refreshTtl(key);
        if (usedAfterReserve != null && usedAfterReserve > quota) {
            stringRedisTemplate.opsForValue().increment(key, -reserve);
            throw quotaExceeded(usedAfterReserve - reserve, quota);
        }
    }

    public void reconcile(Long tenantId, long reservedTokens, long actualTokens) {
        if (tenantId == null) {
            return;
        }
        long reserved = Math.max(reservedTokens, 1L);
        long actual = Math.max(actualTokens, 0L);
        if (actual == reserved) {
            return;
        }
        String key = monthlyKey(tenantId);
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            return;
        }
        stringRedisTemplate.opsForValue().increment(key, actual - reserved);
        refreshTtl(key);
    }

    public void releaseReservation(Long tenantId) {
        releaseReservation(tenantId, DEFAULT_RESERVE);
    }

    public void releaseReservation(Long tenantId, long reservedTokens) {
        if (tenantId == null) {
            return;
        }
        long reserve = Math.max(reservedTokens, 1L);
        String key = monthlyKey(tenantId);
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            return;
        }
        stringRedisTemplate.opsForValue().increment(key, -reserve);
        refreshTtl(key);
    }

    private void ensureInitialized(String key, Long tenantId) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            return;
        }
        YearMonth current = YearMonth.now();
        Long dbUsed = tokenUsageMapper.sumTokensBetween(tenantId, current.atDay(1), current.atEndOfMonth());
        long used = dbUsed != null ? dbUsed : 0L;
        Boolean created = stringRedisTemplate.opsForValue().setIfAbsent(key, Long.toString(used), ttlUntilMonthEnd());
        if (Boolean.FALSE.equals(created)) {
            return;
        }
        refreshTtl(key);
    }

    private void refreshTtl(String key) {
        stringRedisTemplate.expire(key, ttlUntilMonthEnd());
    }

    private Duration ttlUntilMonthEnd() {
        LocalDate lastDay = YearMonth.now().atEndOfMonth();
        long seconds = Math.max(
                Duration.between(
                        java.time.LocalDateTime.now(),
                        lastDay.plusDays(1).atStartOfDay()).getSeconds(),
                TimeUnit.HOURS.toSeconds(1));
        return Duration.ofSeconds(seconds);
    }

    private String monthlyKey(Long tenantId) {
        return KEY_PREFIX + tenantId + ":" + YearMonth.now();
    }

    private BusinessException quotaExceeded(long used, long quota) {
        return new BusinessException(
                "本月 Token 配额已用尽（" + used + "/" + quota + "），请升级套餐或等待下月重置");
    }
}
