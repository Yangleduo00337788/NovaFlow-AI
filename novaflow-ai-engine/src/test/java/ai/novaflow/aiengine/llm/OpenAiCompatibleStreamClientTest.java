package ai.novaflow.aiengine.llm;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenAiCompatibleStreamClientTest {

    @Test
    void messageCreatesSingleEntry() {
        List<Map<String, String>> messages = OpenAiCompatibleStreamClient.message("user", "hello");
        assertEquals(1, messages.size());
        assertEquals("user", messages.get(0).get("role"));
        assertEquals("hello", messages.get(0).get("content"));
    }

    @Test
    void appendMessagePreservesExistingEntries() {
        List<Map<String, String>> messages = OpenAiCompatibleStreamClient.message("system", "prompt");
        List<Map<String, String>> next = OpenAiCompatibleStreamClient.appendMessage(messages, "user", "hi");
        assertEquals(2, next.size());
        assertEquals("system", next.get(0).get("role"));
        assertEquals("user", next.get(1).get("role"));
    }

    @Test
    void totalTokensUsesExplicitTotalWhenPresent() {
        OpenAiCompatibleStreamClient.TokenUsageSummary usage = new OpenAiCompatibleStreamClient.TokenUsageSummary();
        usage.inputTokens = 10;
        usage.outputTokens = 20;
        usage.totalTokens = 99;
        assertEquals(99, usage.totalTokens());
    }

    @Test
    void totalTokensFallsBackToInputPlusOutput() {
        OpenAiCompatibleStreamClient.TokenUsageSummary usage = new OpenAiCompatibleStreamClient.TokenUsageSummary();
        usage.inputTokens = 12;
        usage.outputTokens = 8;
        assertEquals(20, usage.totalTokens());
    }
}
