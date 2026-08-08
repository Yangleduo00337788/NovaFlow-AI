package ai.novaflow.prompt.domain.vo;

import ai.novaflow.prompt.domain.PromptVariable;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PromptTemplateVO {

    private Long id;
    private String templateName;
    private String description;
    private String category;
    private String content;
    private List<PromptVariable> variables;
    private String visibility;
    private Integer currentVersion;
    private Integer usageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
