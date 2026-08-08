package ai.novaflow.prompt.domain;

import lombok.Data;

@Data
public class PromptVariable {

    private String name;
    private String description;
    private String defaultValue;
    private Boolean required;
}
