package ai.novaflow.prompt.domain.dto;

import ai.novaflow.prompt.domain.PromptVariable;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class PromptTemplateSaveRequest {

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    private String description;

    private String category = "custom";

    @NotBlank(message = "Prompt 内容不能为空")
    private String content;

    private List<PromptVariable> variables;

    private String visibility = "private";

    private String changeLog;
}
