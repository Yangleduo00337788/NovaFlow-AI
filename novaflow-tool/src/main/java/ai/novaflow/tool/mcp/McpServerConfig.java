package ai.novaflow.tool.mcp;

import ai.novaflow.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class McpServerConfig {

    private String transportType;
    private String command;
    private List<String> args;
    private Map<String, String> env;
    private String endpoint;

    public static McpServerConfig parse(ObjectMapper objectMapper, String serverName, String configJson) {
        if (!StringUtils.hasText(configJson)) {
            throw new BusinessException("MCP 服务配置不能为空");
        }
        try {
            JsonNode root = objectMapper.readTree(configJson.trim());
            JsonNode serverNode = resolveServerNode(root, serverName);
            if (serverNode == null) {
                serverNode = root;
            }

            String command = textOrNull(serverNode.get("command"));
            List<String> args = readStringList(serverNode.get("args"));
            Map<String, String> env = readStringMap(serverNode.get("env"));
            String endpoint = textOrNull(serverNode.get("url"));
            if (!StringUtils.hasText(endpoint)) {
                endpoint = textOrNull(serverNode.get("endpoint"));
            }

            String transportType = textOrNull(serverNode.get("transportType"));
            if (!StringUtils.hasText(transportType)) {
                transportType = StringUtils.hasText(command) ? "stdio" : "sse";
            }

            if ("stdio".equalsIgnoreCase(transportType)) {
                if (!StringUtils.hasText(command)) {
                    throw new BusinessException("stdio 配置缺少 command 字段");
                }
            } else if (!StringUtils.hasText(endpoint)) {
                throw new BusinessException("远程 MCP 配置缺少 url/endpoint 字段");
            }

            return McpServerConfig.builder()
                    .transportType(transportType.trim().toLowerCase())
                    .command(command)
                    .args(args != null ? args : List.of())
                    .env(env != null ? env : Map.of())
                    .endpoint(endpoint)
                    .build();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("MCP 服务配置 JSON 格式无效");
        }
    }

    public String toStorageJson(ObjectMapper objectMapper) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("transportType", transportType);
            if (StringUtils.hasText(command)) {
                payload.put("command", command);
            }
            if (args != null && !args.isEmpty()) {
                payload.put("args", args);
            }
            if (env != null && !env.isEmpty()) {
                payload.put("env", env);
            }
            if (StringUtils.hasText(endpoint)) {
                payload.put("url", endpoint);
            }
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new BusinessException("MCP 服务配置无效");
        }
    }

    public String commandSummary() {
        if (!StringUtils.hasText(command)) {
            return endpoint != null ? endpoint : "";
        }
        StringBuilder builder = new StringBuilder(command.trim());
        if (args != null) {
            for (String arg : args) {
                builder.append(' ').append(arg);
            }
        }
        return builder.toString().trim();
    }

    private static JsonNode resolveServerNode(JsonNode root, String serverName) {
        if (root == null || root.isNull()) {
            return null;
        }
        JsonNode mcpServers = root.get("mcpServers");
        if (mcpServers == null || mcpServers.isNull()) {
            return root.has("command") || root.has("url") || root.has("endpoint") ? root : null;
        }
        if (StringUtils.hasText(serverName) && mcpServers.has(serverName)) {
            return mcpServers.get(serverName);
        }
        if (mcpServers.size() == 1) {
            return mcpServers.elements().next();
        }
        if (StringUtils.hasText(serverName)) {
            throw new BusinessException("配置中未找到名为 \"" + serverName.trim() + "\" 的 MCP 服务");
        }
        throw new BusinessException("mcpServers 包含多个服务，请填写对应的服务名称");
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private static List<String> readStringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item != null && !item.isNull()) {
                values.add(item.asText());
            }
        }
        return values;
    }

    private static Map<String, String> readStringMap(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> {
            if (entry.getValue() != null && !entry.getValue().isNull()) {
                values.put(entry.getKey(), entry.getValue().asText());
            }
        });
        return values;
    }
}
