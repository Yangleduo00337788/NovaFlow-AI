package ai.novaflow.common.util;

import ai.novaflow.common.domain.RetrievalConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class RetrievalConfigUtils {

    private RetrievalConfigUtils() {
    }

    public static RetrievalConfig parse(ObjectMapper objectMapper, String json) {
        if (json == null || json.isBlank()) {
            return new RetrievalConfig();
        }
        try {
            RetrievalConfig config = objectMapper.readValue(json, RetrievalConfig.class);
            return config != null ? config : new RetrievalConfig();
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse retrieval_config JSON: {}", json, e);
            return new RetrievalConfig();
        }
    }

    public static String serialize(ObjectMapper objectMapper, RetrievalConfig config) {
        if (config == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize retrieval_config", e);
            return null;
        }
    }
}
