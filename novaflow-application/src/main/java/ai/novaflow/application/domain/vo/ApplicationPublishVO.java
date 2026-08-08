package ai.novaflow.application.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApplicationPublishVO {

    private Long applicationId;
    private Integer publishStatus;
    private Long defaultAgentId;
    private String defaultAgentName;
    private LocalDateTime publishedAt;
    private String chatEndpoint;
    private String streamEndpoint;
    private String embedPath;
}
