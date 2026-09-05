package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlatformProviderStatVO {

    private String providerCode;
    private long count;
}
