package ai.novaflow.aiengine.llm;

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
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RerankClient {

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public List<ScoredIndex> rerank(
            ResolvedModelConfig config,
            String query,
            List<String> documents,
            int topN) throws Exception {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", config.getModelName());
        body.put("query", query);
        ArrayNode docs = body.putArray("documents");
        documents.forEach(docs::add);
        body.put("top_n", Math.min(topN, documents.size()));

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(resolveEndpoint(config.getBaseUrl())))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        if (StringUtils.hasText(config.getApiKey())) {
            requestBuilder.header("Authorization", "Bearer " + config.getApiKey());
        }

        HttpResponse<String> response = httpClient.send(
                requestBuilder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Rerank 调用失败 (" + response.statusCode() + "): " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode results = root.path("results");
        if (!results.isArray() || results.isEmpty()) {
            results = root.path("output").path("results");
        }
        if (!results.isArray()) {
            throw new IllegalStateException("Rerank 响应格式不支持: " + response.body());
        }

        List<ScoredIndex> scored = new ArrayList<>();
        for (JsonNode item : results) {
            int index = item.path("index").asInt(item.path("document").path("index").asInt(-1));
            float score = (float) item.path("relevance_score").asDouble(item.path("score").asDouble(0));
            if (index >= 0) {
                scored.add(new ScoredIndex(index, score));
            }
        }
        scored.sort(Comparator.comparing(ScoredIndex::getScore, Comparator.nullsLast(Comparator.reverseOrder())));
        return scored;
    }

    private String resolveEndpoint(String baseUrl) {
        String normalized = baseUrl != null ? baseUrl.trim() : "";
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/rerank")) {
            return normalized;
        }
        if (normalized.endsWith("/v1")) {
            return normalized + "/rerank";
        }
        return normalized + "/v1/rerank";
    }

    @Data
    public static class ScoredIndex {
        private final int index;
        private final Float score;
    }
}
