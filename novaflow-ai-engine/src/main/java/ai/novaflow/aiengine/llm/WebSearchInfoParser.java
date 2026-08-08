package ai.novaflow.aiengine.llm;

import ai.novaflow.aiengine.agent.WebSearchSource;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

final class WebSearchInfoParser {

    private WebSearchInfoParser() {
    }

    static void parseAndEmit(JsonNode root, Map<String, WebSearchSource> accumulator, Consumer<List<WebSearchSource>> onSources) {
        if (root == null || root.isMissingNode()) {
            return;
        }
        appendSearchResults(root.path("search_info"), accumulator);
        appendSearchResults(root.path("output").path("search_info"), accumulator);
        JsonNode choices = root.path("choices");
        if (choices.isArray()) {
            for (JsonNode choice : choices) {
                appendSearchResults(choice.path("search_info"), accumulator);
                appendSearchResults(choice.path("message").path("search_info"), accumulator);
                appendSearchResults(choice.path("delta").path("search_info"), accumulator);
            }
        }
        if (!accumulator.isEmpty()) {
            onSources.accept(new ArrayList<>(accumulator.values()));
        }
    }

    private static void appendSearchResults(JsonNode searchInfo, Map<String, WebSearchSource> accumulator) {
        JsonNode searchResults = searchInfo.path("search_results");
        if (!searchResults.isArray()) {
            return;
        }
        for (JsonNode item : searchResults) {
            String url = text(item, "url");
            String title = text(item, "title");
            if (!StringUtils.hasText(url) && !StringUtils.hasText(title)) {
                continue;
            }
            String key = StringUtils.hasText(url) ? url : title;
            if (accumulator.containsKey(key)) {
                continue;
            }
            accumulator.put(key, WebSearchSource.builder()
                    .index(item.path("index").asInt(accumulator.size() + 1))
                    .title(title)
                    .url(url)
                    .snippet(text(item, "snippet", "content", "text"))
                    .build());
        }
    }

    private static String text(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asText("");
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }
}
