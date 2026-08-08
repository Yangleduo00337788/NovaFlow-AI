package ai.novaflow.prompt.util;

import ai.novaflow.prompt.domain.PromptVariable;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PromptVariableUtils {

    private final ObjectMapper objectMapper;

    public String serialize(List<PromptVariable> variables) {
        if (variables == null || variables.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (Exception e) {
            throw new IllegalStateException("变量序列化失败", e);
        }
    }

    public List<PromptVariable> parse(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<PromptVariable>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
