package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlatformTrendPointVO {

    private String label;
    private long tokens;
}
