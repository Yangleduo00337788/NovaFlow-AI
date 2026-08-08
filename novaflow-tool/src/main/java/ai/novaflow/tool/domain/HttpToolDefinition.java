package ai.novaflow.tool.domain;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class HttpToolDefinition {

    private String name;
    private String description;
    private Map<String, Object> inputSchema;
    private String method = "GET";
    private String url;
    private String bodyTemplate;
    private Map<String, String> headers = new HashMap<>();
}
