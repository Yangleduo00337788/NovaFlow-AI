package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlatformTenantUsageVO {

    private Long tenantId;
    private String tenantName;
    private long calls;
    private long tokens;
}
