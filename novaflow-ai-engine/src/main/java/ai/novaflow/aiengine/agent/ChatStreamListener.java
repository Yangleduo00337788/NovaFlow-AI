package ai.novaflow.aiengine.agent;

@FunctionalInterface
public interface ChatStreamListener {

    void onToken(String token);

    default void onComplete(ChatExecuteResult result) {
    }

    default void onError(Throwable error) {
    }
}
