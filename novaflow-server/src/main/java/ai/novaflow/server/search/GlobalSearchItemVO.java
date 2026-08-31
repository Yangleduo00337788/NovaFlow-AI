package ai.novaflow.server.search;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GlobalSearchItemVO {

    private String type;
    private Long id;
    private String title;
    private String subtitle;
    private String path;
}
