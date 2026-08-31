package ai.novaflow.common.telemetry;

import java.util.Map;

public final class NoopNovaFlowTracer implements NovaFlowTracer {

    public static final NovaFlowTracer INSTANCE = new NoopNovaFlowTracer();

    private NoopNovaFlowTracer() {
    }

    @Override
    public SpanScope startSpan(String spanName, Map<String, String> attributes) {
        return new SpanScope() {
            @Override
            public void recordError(Throwable error) {
            }

            @Override
            public void setAttribute(String key, String value) {
            }

            @Override
            public void close() {
            }
        };
    }
}
