package ai.novaflow.aiengine.llm;

import ai.novaflow.model.domain.ModelExtraParametersBuilder;
import ai.novaflow.model.domain.ResolvedModelConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiCompatibleChatClient {

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public ChatCompletionResponse chat(
            ResolvedModelConfig config,
            ArrayNode messages,
            ArrayNode tools) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", config.getModelName());
        body.put("stream", false);
        if (config.getTemperature() != null) {
            body.put("temperature", config.getTemperature().doubleValue());
        }
        if (config.getMaxTokens() != null) {
            body.put("max_tokens", config.getMaxTokens());
        }
        body.set("messages", messages);
        if (tools != null && !tools.isEmpty()) {
            body.set("tools", tools);
        }

        Map<String, Object> extra = ModelExtraParametersBuilder.build(config);
        for (Map.Entry<String, Object> entry : extra.entrySet()) {
            body.set(entry.getKey(), objectMapper.valueToTree(entry.getValue()));
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(resolveEndpoint(config.getBaseUrl())))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        if (StringUtils.hasText(config.getApiKey())) {
            requestBuilder.header("Authorization", "Bearer " + config.getApiKey());
        }

        HttpResponse<String> response = httpClient.send(
                requestBuilder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("模型调用失败 (" + response.statusCode() + "): " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode message = root.path("choices").path(0).path("message");
        ChatCompletionResponse result = new ChatCompletionResponse();
        result.setContent(message.path("content").asText(""));
        result.setReasoningContent(message.path("reasoning_content").asText(""));
        result.setFinishReason(root.path("choices").path(0).path("finish_reason").asText(""));

        JsonNode toolCalls = message.path("tool_calls");
        if (toolCalls.isArray()) {
            List<ToolCallItem> items = new ArrayList<>();
            for (JsonNode toolCall : toolCalls) {
                ToolCallItem item = new ToolCallItem();
                item.setId(toolCall.path("id").asText(""));
                item.setName(toolCall.path("function").path("name").asText(""));
                item.setArguments(toolCall.path("function").path("arguments").asText("{}"));
                items.add(item);
            }
            result.setToolCalls(items);
        }

        JsonNode usageNode = root.path("usage");
        OpenAiCompatibleStreamClient.TokenUsageSummary usage = new OpenAiCompatibleStreamClient.TokenUsageSummary();
        usage.inputTokens = usageNode.path("prompt_tokens").asInt(0);
        usage.outputTokens = usageNode.path("completion_tokens").asInt(0);
        usage.totalTokens = usageNode.path("total_tokens").asInt(0);
        result.setUsage(usage);
        return result;
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

    @Data
    public static class ChatCompletionResponse {
        private String content;
        private String reasoningContent;
        private String finishReason;
        private List<ToolCallItem> toolCalls = List.of();
        private OpenAiCompatibleStreamClient.TokenUsageSummary usage;
    }

    @Data
    public static class ToolCallItem {
        private String id;
        private String name;
        private String arguments;
    }
}
