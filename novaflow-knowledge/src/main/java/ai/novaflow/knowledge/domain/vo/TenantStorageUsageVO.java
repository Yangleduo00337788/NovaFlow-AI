package ai.novaflow.knowledge.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantStorageUsageVO {

    private long usedBytes;
    private Integer maxStorageMb;
    private Integer usedPercent;
    private boolean quotaExceeded;
}
