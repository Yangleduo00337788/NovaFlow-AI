package ai.novaflow.aiengine.llm;

import ai.novaflow.aiengine.agent.WebSearchSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSearchInfoParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void nullRootDoesNotEmit() {
        Map<String, WebSearchSource> accumulator = new LinkedHashMap<>();
        List<WebSearchSource> emitted = capture(accumulator, null);
        assertTrue(emitted.isEmpty());
        assertTrue(accumulator.isEmpty());
    }

    @Test
    void parsesRootSearchResults() throws Exception {
        String json = """
                {
                  "search_info": {
                    "search_results": [
                      {"index": 1, "title": "NovaFlow", "url": "https://example.com/a", "snippet": "intro"}
                    ]
                  }
                }
                """;
        Map<String, WebSearchSource> accumulator = new LinkedHashMap<>();
        List<WebSearchSource> emitted = capture(accumulator, objectMapper.readTree(json));
        assertEquals(1, emitted.size());
        assertEquals("NovaFlow", emitted.get(0).getTitle());
        assertEquals("https://example.com/a", emitted.get(0).getUrl());
        assertEquals("intro", emitted.get(0).getSnippet());
    }

    @Test
    void parsesNestedChoiceSearchResults() throws Exception {
        String json = """
                {
                  "choices": [
                    {
                      "message": {
                        "search_info": {
                          "search_results": [
                            {"title": "Nested", "url": "https://nested.example"}
                          ]
                        }
                      }
                    }
                  ]
                }
                """;
        Map<String, WebSearchSource> accumulator = new LinkedHashMap<>();
        List<WebSearchSource> emitted = capture(accumulator, objectMapper.readTree(json));
        assertEquals(1, emitted.size());
        assertEquals("Nested", emitted.get(0).getTitle());
    }

    @Test
    void skipsBlankItemsAndDedupesByUrl() throws Exception {
        String json = """
                {
                  "search_info": {
                    "search_results": [
                      {"title": "", "url": ""},
                      {"title": "A", "url": "https://dup.example", "content": "first"},
                      {"title": "B", "url": "https://dup.example", "text": "second"}
                    ]
                  }
                }
                """;
        Map<String, WebSearchSource> accumulator = new LinkedHashMap<>();
        List<WebSearchSource> emitted = capture(accumulator, objectMapper.readTree(json));
        assertEquals(1, emitted.size());
        assertEquals("first", emitted.get(0).getSnippet());
    }

    private static List<WebSearchSource> capture(Map<String, WebSearchSource> accumulator, com.fasterxml.jackson.databind.JsonNode root) {
        List<WebSearchSource> emitted = new ArrayList<>();
        WebSearchInfoParser.parseAndEmit(root, accumulator, emitted::addAll);
        return emitted;
    }
}
