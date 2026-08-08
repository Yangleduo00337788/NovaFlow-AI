package ai.novaflow.aiengine.agent;

import java.util.List;

@FunctionalInterface
public interface ChatStreamListener {
    void onToken(String token);

    default void onThinkingToken(String token) {
    }

    default void onToolCall(String toolName, String arguments) {
    }

    default void onToolResult(String toolName, String result) {
    }

    default void onWebSearchSources(List<WebSearchSource> sources) {
    }

    default void onComplete(ChatExecuteResult result) {
    }

    default void onError(Throwable error) {
    }
}
