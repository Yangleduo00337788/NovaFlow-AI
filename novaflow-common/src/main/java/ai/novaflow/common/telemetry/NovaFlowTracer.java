package ai.novaflow.common.telemetry;

import java.util.Map;

public interface NovaFlowTracer {

    SpanScope startSpan(String spanName, Map<String, String> attributes);

    interface SpanScope extends AutoCloseable {
        void recordError(Throwable error);

        void setAttribute(String key, String value);

        @Override
        void close();
    }
}
