package ai.novaflow.tool.util;

import ai.novaflow.tool.domain.HttpToolDefinition;
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
