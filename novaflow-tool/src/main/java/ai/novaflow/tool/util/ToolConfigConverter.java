package ai.novaflow.tool.util;

import ai.novaflow.tool.domain.HttpToolDefinition;
import ai.novaflow.tool.domain.McpToolDefinition;
import ai.novaflow.tool.domain.SkillDefinition;
import ai.novaflow.tool.entity.ToolDefinitionEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ToolConfigConverter {

    private final ObjectMapper objectMapper;

    public HttpToolDefinition toHttpTool(ToolDefinitionEntity entity) {
        HttpToolDefinition tool = new HttpToolDefinition();
        tool.setName(entity.getToolName());
        tool.setDescription(entity.getDescription());
        if (!StringUtils.hasText(entity.getToolConfig())) {
            return tool;
        }
        if ("mcp".equalsIgnoreCase(entity.getToolType())) {
            McpToolDefinition mcpTool = toMcpTool(entity);
            tool.setToolType("mcp");
            tool.setMcpServerId(mcpTool.getMcpServerId());
            tool.setMcpToolName(mcpTool.getMcpToolName());
            if (mcpTool.getInputSchema() != null) {
                tool.setInputSchema(mcpTool.getInputSchema());
            }
            return tool;
        }
        try {
            Map<String, Object> config = objectMapper.readValue(
                    entity.getToolConfig(),
                    new TypeReference<Map<String, Object>>() {}
            );
            tool.setMethod(stringValue(config.get("method"), "GET"));
            tool.setUrl(stringValue(config.get("url"), null));
            tool.setBodyTemplate(stringValue(config.get("bodyTemplate"), null));
            Object headers = config.get("headers");
            if (headers instanceof Map<?, ?> headerMap) {
                Map<String, String> parsed = new HashMap<>();
                headerMap.forEach((key, value) -> {
                    if (key != null && value != null) {
                        parsed.put(String.valueOf(key), String.valueOf(value));
                    }
                });
                tool.setHeaders(parsed);
            }
            Object inputSchema = config.get("inputSchema");
            if (inputSchema instanceof Map<?, ?> schemaMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typed = (Map<String, Object>) schemaMap;
                tool.setInputSchema(typed);
            }
        } catch (Exception ignored) {
            // 保持最小可用定义
        }
        return tool;
    }

    public McpToolDefinition toMcpTool(ToolDefinitionEntity entity) {
        McpToolDefinition tool = McpToolDefinition.builder().build();
        if (!StringUtils.hasText(entity.getToolConfig())) {
            return tool;
        }
        try {
            Map<String, Object> config = objectMapper.readValue(
                    entity.getToolConfig(),
                    new TypeReference<Map<String, Object>>() {}
            );
            Object mcpServerId = config.get("mcpServerId");
            if (mcpServerId instanceof Number number) {
                tool.setMcpServerId(number.longValue());
            } else if (mcpServerId != null) {
                tool.setMcpServerId(Long.parseLong(String.valueOf(mcpServerId)));
            }
            tool.setMcpToolName(stringValue(config.get("mcpToolName"), null));
            tool.setSourceServerName(stringValue(config.get("sourceServerName"), null));
            Object inputSchema = config.get("inputSchema");
            if (inputSchema instanceof Map<?, ?> schemaMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typed = (Map<String, Object>) schemaMap;
                tool.setInputSchema(typed);
            }
        } catch (Exception ignored) {
            // 保持最小可用定义
        }
        return tool;
    }

    public SkillDefinition toSkill(ToolDefinitionEntity entity) {
        SkillDefinition.SkillDefinitionBuilder builder = SkillDefinition.builder();
        if (!StringUtils.hasText(entity.getToolConfig())) {
            return builder.build();
        }
        try {
            Map<String, Object> config = objectMapper.readValue(
                    entity.getToolConfig(),
                    new TypeReference<Map<String, Object>>() {}
            );
            builder.fileName(stringValue(config.get("fileName"), null));
            builder.content(stringValue(config.get("content"), null));
        } catch (Exception ignored) {
            // 保持最小可用定义
        }
        return builder.build();
    }

    public String serializeSkill(SkillDefinition skill) {
        Map<String, Object> config = new HashMap<>();
        config.put("fileName", skill.getFileName());
        config.put("content", skill.getContent());
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            throw new IllegalStateException("Skill 配置序列化失败", e);
        }
    }

    public String serializeMcpConfig(McpToolDefinition tool) {
        Map<String, Object> config = new HashMap<>();
        config.put("mcpServerId", tool.getMcpServerId());
        config.put("mcpToolName", tool.getMcpToolName());
        if (StringUtils.hasText(tool.getSourceServerName())) {
            config.put("sourceServerName", tool.getSourceServerName());
        }
        if (tool.getInputSchema() != null && !tool.getInputSchema().isEmpty()) {
            config.put("inputSchema", tool.getInputSchema());
        }
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            throw new IllegalStateException("MCP 工具配置序列化失败", e);
        }
    }

    public String serializeConfig(HttpToolDefinition tool) {
        Map<String, Object> config = new HashMap<>();
        config.put("method", StringUtils.hasText(tool.getMethod()) ? tool.getMethod() : "GET");
        config.put("url", tool.getUrl());
        if (StringUtils.hasText(tool.getBodyTemplate())) {
            config.put("bodyTemplate", tool.getBodyTemplate());
        }
        if (tool.getHeaders() != null && !tool.getHeaders().isEmpty()) {
            config.put("headers", tool.getHeaders());
        }
        if (tool.getInputSchema() != null && !tool.getInputSchema().isEmpty()) {
            config.put("inputSchema", tool.getInputSchema());
        }
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            throw new IllegalStateException("工具配置序列化失败", e);
        }
    }

    private String stringValue(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? text : defaultValue;
    }
}
