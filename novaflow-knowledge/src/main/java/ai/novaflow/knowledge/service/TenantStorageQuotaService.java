package ai.novaflow.knowledge.service;

import ai.novaflow.common.context.TenantContexts;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.knowledge.domain.vo.TenantStorageUsageVO;
import ai.novaflow.knowledge.mapper.DocumentMapper;
import ai.novaflow.tenant.entity.TenantEntity;
import ai.novaflow.tenant.mapper.TenantMapper;
import ai.novaflow.tenant.support.TenantQuotas;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantStorageQuotaService {

    private final DocumentMapper documentMapper;
    private final TenantMapper tenantMapper;

    public TenantStorageUsageVO usage() {
        Long tenantId = TenantContexts.requireTenantId();
        return snapshot(tenantId);
    }

    public TenantStorageUsageVO snapshot(Long tenantId) {
        TenantEntity tenant = tenantMapper.selectOneById(tenantId);
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }
        long usedBytes = usedBytes(tenantId);
        Integer maxStorageMb = tenant.getMaxStorageMb();
        Integer usedPercent = calcPercent(usedBytes, maxStorageMb);
        boolean quotaExceeded = maxStorageMb != null
                && maxStorageMb > 0
                && usedBytes >= maxStorageMb.longValue() * 1024L * 1024L;
        return TenantStorageUsageVO.builder()
                .usedBytes(usedBytes)
                .maxStorageMb(maxStorageMb)
                .usedPercent(usedPercent)
                .quotaExceeded(quotaExceeded)
                .build();
    }

    public void assertCanUpload(Long tenantId, long incomingBytes) {
        TenantEntity tenant = tenantMapper.selectOneById(tenantId);
        if (tenant == null) {
            return;
        }
        int maxStorageMb = tenant.getMaxStorageMb() != null && tenant.getMaxStorageMb() > 0
                ? tenant.getMaxStorageMb()
                : 0;
        if (maxStorageMb <= 0) {
            return;
        }
        long limitBytes = maxStorageMb * 1024L * 1024L;
        TenantQuotas.assertStorageWithinLimit(usedBytes(tenantId), incomingBytes, limitBytes);
    }

    private long usedBytes(Long tenantId) {
        Long used = documentMapper.sumFileSizeByTenant(tenantId);
        return used != null ? used : 0L;
    }

    private Integer calcPercent(long usedBytes, Integer maxStorageMb) {
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
