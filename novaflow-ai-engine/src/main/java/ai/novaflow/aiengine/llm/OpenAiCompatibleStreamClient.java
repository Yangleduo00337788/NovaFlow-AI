package ai.novaflow.aiengine.llm;

import ai.novaflow.model.domain.ModelExtraParametersBuilder;
import ai.novaflow.model.domain.ResolvedModelConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class OpenAiCompatibleStreamClient {

    private final ObjectMapper objectMapper;

  private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public void streamChat(
            ResolvedModelConfig config,
            List<Map<String, String>> messages,
            Consumer<String> onThinkingToken,
            Consumer<String> onContentToken,
            Consumer<TokenUsageSummary> onUsage) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", config.getModelName());
        body.put("stream", true);
        if (config.getTemperature() != null) {
            body.put("temperature", config.getTemperature().doubleValue());
        }
        if (config.getMaxTokens() != null) {
            body.put("max_tokens", config.getMaxTokens());
        }

        ArrayNode messageArray = body.putArray("messages");
        for (Map<String, String> message : messages) {
            ObjectNode item = messageArray.addObject();
            item.put("role", message.get("role"));
            item.put("content", message.get("content"));
        }

        Map<String, Object> extra = ModelExtraParametersBuilder.build(config);
        for (Map.Entry<String, Object> entry : extra.entrySet()) {
            body.set(entry.getKey(), objectMapper.valueToTree(entry.getValue()));
        }

        String endpoint = resolveEndpoint(config.getBaseUrl());
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        if (StringUtils.hasText(config.getApiKey())) {
            requestBuilder.header("Authorization", "Bearer " + config.getApiKey());
        }

        HttpResponse<java.io.InputStream> response = httpClient.send(
                requestBuilder.build(),
                HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new IllegalStateException("模型调用失败 (" + response.statusCode() + "): " + errorBody);
        }

        TokenUsageSummary usage = new TokenUsageSummary();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring(5).trim();
                if (!StringUtils.hasText(payload) || "[DONE]".equals(payload)) {
                    continue;
                }
                JsonNode root = objectMapper.readTree(payload);
                JsonNode choices = root.path("choices");
                if (!choices.isArray() || choices.isEmpty()) {
                    continue;
                }
                JsonNode delta = choices.get(0).path("delta");
                String thinkingToken = delta.path("reasoning_content").asText("");
                String contentToken = delta.path("content").asText("");
                if (StringUtils.hasText(thinkingToken)) {
                    onThinkingToken.accept(thinkingToken);
                }
                if (StringUtils.hasText(contentToken)) {
                    onContentToken.accept(contentToken);
                }
                JsonNode usageNode = root.path("usage");
                if (!usageNode.isMissingNode()) {
                    usage.inputTokens = usageNode.path("prompt_tokens").asInt(usage.inputTokens);
                    usage.outputTokens = usageNode.path("completion_tokens").asInt(usage.outputTokens);
                    usage.totalTokens = usageNode.path("total_tokens").asInt(usage.totalTokens);
                }
            }
        }
        onUsage.accept(usage);
    }

    private String resolveEndpoint(String baseUrl) {
        String normalized = baseUrl != null ? baseUrl.trim() : "";
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalStateException("模型 baseUrl 未配置");
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/chat/completions")) {
            return normalized;
        }
        return normalized + "/chat/completions";
    }

    public static List<Map<String, String>> message(String role, String content) {
        return List.of(Map.of("role", role, "content", content));
    }

    public static List<Map<String, String>> appendMessage(List<Map<String, String>> messages, String role, String content) {
        List<Map<String, String>> next = new ArrayList<>(messages);
        next.add(Map.of("role", role, "content", content));
        return next;
    }

    public static class TokenUsageSummary {
        int inputTokens;
        int outputTokens;
        int totalTokens;

        public int inputTokens() {
            return inputTokens;
        }

        public int outputTokens() {
            return outputTokens;
        }

        public int totalTokens() {
            return totalTokens > 0 ? totalTokens : inputTokens + outputTokens;
        }

        public void updateFrom(TokenUsageSummary other) {
            if (other == null) {
                return;
            }
            this.inputTokens = other.inputTokens;
            this.outputTokens = other.outputTokens;
            this.totalTokens = other.totalTokens;
        }
    }
}
