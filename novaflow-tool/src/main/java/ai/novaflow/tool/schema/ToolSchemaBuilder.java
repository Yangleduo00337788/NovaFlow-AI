package ai.novaflow.tool.schema;

import ai.novaflow.tool.domain.HttpToolDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Component
public class ToolSchemaBuilder {

    private final ObjectMapper objectMapper;

    public ToolSchemaBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ArrayNode toOpenAiTools(List<HttpToolDefinition> tools) {
        ArrayNode array = objectMapper.createArrayNode();
        if (tools == null) {
            return array;
        }
        for (HttpToolDefinition tool : tools) {
            if (!StringUtils.hasText(tool.getName())) {
                continue;
            }
            ObjectNode item = array.addObject();
            item.put("type", "function");
            ObjectNode function = item.putObject("function");
            function.put("name", tool.getName().trim());
            function.put("description", StringUtils.hasText(tool.getDescription()) ? tool.getDescription() : tool.getName());
            if (tool.getInputSchema() != null && !tool.getInputSchema().isEmpty()) {
                function.set("parameters", objectMapper.valueToTree(tool.getInputSchema()));
            } else {
                function.set("parameters", defaultParameters());
            }
        }
        return array;
    }

    private ObjectNode defaultParameters() {
        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        ObjectNode properties = parameters.putObject("properties");
        properties.putObject("input").put("type", "string").put("description", "工具输入");
        return parameters;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> parseArguments(String argumentsJson) {
        if (!StringUtils.hasText(argumentsJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(argumentsJson, Map.class);
        } catch (Exception e) {
            return Map.of("input", argumentsJson);
        }
    }
}
