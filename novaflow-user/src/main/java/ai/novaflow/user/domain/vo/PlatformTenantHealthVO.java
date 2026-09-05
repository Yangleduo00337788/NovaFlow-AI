package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PlatformTenantHealthVO {

    private Long tenantId;
    private String tenantName;
    private String healthStatus;
    private List<String> reasons;
    private Integer tokenUsedPercent;
    private Integer memberUsedPercent;
    private Integer daysUntilExpiry;
    private Integer status;
}
