package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlatformModelUsageVO {

    private String modelName;
    private String displayName;
    private long calls;
    private long tokens;
}
