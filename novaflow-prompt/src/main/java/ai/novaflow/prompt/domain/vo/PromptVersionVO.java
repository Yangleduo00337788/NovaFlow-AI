package ai.novaflow.prompt.domain.vo;

import ai.novaflow.prompt.domain.PromptVariable;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PromptVersionVO {

    private Long id;
    private Long templateId;
    private Integer version;
    private String content;
    private List<PromptVariable> variables;
    private String changeLog;
    private LocalDateTime publishedAt;
}
