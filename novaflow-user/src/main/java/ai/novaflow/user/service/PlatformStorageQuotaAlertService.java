package ai.novaflow.user.service;

import ai.novaflow.tenant.entity.TenantEntity;
import ai.novaflow.tenant.mapper.TenantMapper;
import ai.novaflow.user.mapper.PlatformStatsMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformStorageQuotaAlertService {

    private final TenantMapper tenantMapper;
    private final PlatformStatsMapper platformStatsMapper;
    private final PlatformSystemConfigService platformSystemConfigService;
    private final PlatformAlertDispatchService platformAlertDispatchService;
    private final StringRedisTemplate stringRedisTemplate;

    public void scanAndNotify() {
        int warnPercent = platformSystemConfigService.getStorageWarnPercent();
        List<String> channels = platformSystemConfigService.getStorageQuotaAlertChannels();
        if (channels.isEmpty()) {
            return;
        }
        List<TenantEntity> tenants = tenantMapper.selectListByQuery(
                QueryWrapper.create().eq("is_deleted", 0).eq("status", 1));
        LocalDate today = LocalDate.now();
        for (TenantEntity tenant : tenants) {
            Integer maxStorageMb = tenant.getMaxStorageMb();
            if (maxStorageMb == null || maxStorageMb <= 0) {
                continue;
            }
            long usedBytes = safeLong(platformStatsMapper.sumStorageBytesByTenant(tenant.getId()));
            Integer usedPercent = calcStoragePercent(usedBytes, maxStorageMb);
            if (usedPercent == null) {
                continue;
            }
            if (usedPercent >= 100) {
                notifyOnce(tenant, usedPercent, warnPercent, usedBytes, maxStorageMb, "critical", today);
            } else if (usedPercent >= warnPercent) {
                notifyOnce(tenant, usedPercent, warnPercent, usedBytes, maxStorageMb, "warning", today);
            }
        }
    }

    private void notifyOnce(
            TenantEntity tenant,
            int usedPercent,
            int warnPercent,
            long usedBytes,
            int maxStorageMb,
            String severity,
            LocalDate today) {
        String dedupeKey = "novaflow:platform:notify:storage:"
                + tenant.getId() + ":" + severity + ":" + today;
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(dedupeKey, "1", Duration.ofDays(1));
        if (!Boolean.TRUE.equals(acquired)) {
            return;
        }
        platformAlertDispatchService.dispatchStorageQuotaAlert(
                tenant.getId(),
                tenant.getTenantName(),
                usedPercent,
                warnPercent,
                usedBytes,
                maxStorageMb,
                severity);
    }

    private long safeLong(Long value) {
        return value != null ? value : 0L;
    }

    private Integer calcStoragePercent(long usedBytes, Integer maxStorageMb) {
        if (maxStorageMb == null || maxStorageMb <= 0) {
            return null;
        }
        long limitBytes = maxStorageMb.longValue() * 1024L * 1024L;
        if (limitBytes <= 0) {
            return null;
        }
        return (int) Math.min(100L, (usedBytes * 100L) / limitBytes);
    }
}
