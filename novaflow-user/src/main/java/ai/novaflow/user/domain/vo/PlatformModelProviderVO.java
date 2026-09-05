package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PlatformModelProviderVO {

    private Long id;
    private Long tenantId;
    private String tenantName;
    private String providerCode;
    private String providerName;
    private String baseUrl;
    private String apiKeyMasked;
    private Boolean enabled;
    private Integer modelCount;
    private Integer enabledModelCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
