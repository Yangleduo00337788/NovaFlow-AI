package ai.novaflow.tool.domain.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ToolTestRequest {

    private Map<String, Object> arguments;
}
