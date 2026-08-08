package ai.novaflow.model.service;

import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.model.domain.UpstreamModelDescriptor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ModelUpstreamService {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    public List<UpstreamModelDescriptor> listModels(String baseUrl, String apiKey) {
        return listModels(baseUrl, apiKey, true);
    }

    public List<UpstreamModelDescriptor> listModels(String baseUrl, String apiKey, boolean requiresApiKey) {
        if (requiresApiKey && !StringUtils.hasText(apiKey)) {
            throw new BusinessException("请先配置 API Key");
        }
        if (!StringUtils.hasText(baseUrl)) {
            throw new BusinessException("请先配置 Base URL");
        }

        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        try {
            HttpResponse<String> response = httpClient.send(
                    buildListModelsRequest(normalizedBaseUrl, apiKey),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("获取上游模型失败（HTTP " + response.statusCode() + "）");
            }

            return parseModels(response.body());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("获取上游模型失败：" + ex.getMessage());
        }
    }

    private List<UpstreamModelDescriptor> parseModels(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode data = root.path("data");
        if (!data.isArray() || data.isEmpty()) {
            throw new BusinessException("上游未返回可用模型");
        }

        Set<String> seen = new LinkedHashSet<>();
        List<UpstreamModelDescriptor> models = new ArrayList<>();
        for (JsonNode item : data) {
            String modelName = item.path("id").asText(null);
            if (!StringUtils.hasText(modelName) || !seen.add(modelName)) {
                continue;
            }
            if (!shouldInclude(modelName)) {
                continue;
            }
            String modelType = inferModelType(modelName);
            models.add(UpstreamModelDescriptor.builder()
                    .modelName(modelName)
                    .modelType(modelType)
                    .displayName(toDisplayName(modelName))
                    .build());
        }

        if (models.isEmpty()) {
            throw new BusinessException("上游未返回可用模型");
        }

        models.sort(Comparator.comparing(UpstreamModelDescriptor::getModelType)
                .thenComparing(UpstreamModelDescriptor::getModelName));
        return models;
    }

    private boolean shouldInclude(String modelName) {
        String lower = modelName.toLowerCase(Locale.ROOT);
        return !(lower.startsWith("dall-e")
                || lower.startsWith("whisper")
                || lower.startsWith("tts-")
                || lower.contains("realtime")
                || lower.contains("transcribe")
                || lower.contains("moderation")
                || lower.endsWith("-audio"));
    }

    private String inferModelType(String modelName) {
        String lower = modelName.toLowerCase(Locale.ROOT);
        if (lower.contains("embed")) {
            return "embedding";
        }
        if (lower.contains("rerank")) {
            return "rerank";
        }
        return "chat";
    }

    private String toDisplayName(String modelName) {
        String[] parts = modelName.split("[-_/]");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    private HttpRequest buildListModelsRequest(String normalizedBaseUrl, String apiKey) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(normalizedBaseUrl + "/models"))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .GET();
        if (StringUtils.hasText(apiKey)) {
            builder.header("Authorization", "Bearer " + apiKey);
        }
        return builder.build();
    }

    private String normalizeBaseUrl(String baseUrl) {
        String trimmed = baseUrl.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
