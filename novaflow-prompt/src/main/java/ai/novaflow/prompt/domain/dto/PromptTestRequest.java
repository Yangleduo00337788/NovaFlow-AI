package ai.novaflow.prompt.domain.dto;

import lombok.Data;

import java.util.Map;

@Data
public class PromptTestRequest {

    private Long modelConfigId;
    private Map<String, Object> variables;
    private String userMessage;
}
