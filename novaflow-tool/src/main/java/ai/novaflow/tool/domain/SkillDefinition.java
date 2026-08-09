package ai.novaflow.tool.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillDefinition {

    private String fileName;
    private String toolName;
    private String displayName;
    private String content;
}
