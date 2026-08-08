package ai.novaflow.agent.util;

import ai.novaflow.tool.domain.HttpToolDefinition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class AgentExtraConfigUtils {

    private AgentExtraConfigUtils() {
    }

    public static List<HttpToolDefinition> parseTools(ObjectMapper objectMapper, String extraConfig) {
        if (!StringUtils.hasText(extraConfig)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(extraConfig);
            JsonNode toolsNode = root.path("tools");
            if (!toolsNode.isArray()) {
                return List.of();
            }
            return objectMapper.convertValue(toolsNode, new TypeReference<List<HttpToolDefinition>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    public static String serializeTools(ObjectMapper objectMapper, List<HttpToolDefinition> tools) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.set("tools", objectMapper.valueToTree(tools != null ? tools : new ArrayList<>()));
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"tools\":[]}";
        }
    }
}
