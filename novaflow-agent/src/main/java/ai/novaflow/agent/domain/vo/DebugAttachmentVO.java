package ai.novaflow.agent.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DebugAttachmentVO {

    private String fileName;
    private String content;
    private Integer contentLength;
}
