package ai.novaflow.agent.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WebSearchSourceVO {

    private Integer index;
    private String title;
    private String url;
    private String snippet;
}
