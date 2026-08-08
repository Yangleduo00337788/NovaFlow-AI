package ai.novaflow.aiengine.agent;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WebSearchSource {

    private Integer index;
    private String title;
    private String url;
    private String snippet;
}
