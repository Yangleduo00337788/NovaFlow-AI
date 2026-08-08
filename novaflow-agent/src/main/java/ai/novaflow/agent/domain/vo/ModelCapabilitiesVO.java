package ai.novaflow.agent.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ModelCapabilitiesVO {

    private Boolean supportsDeepThinking;
    private Boolean supportsWebSearch;
}
