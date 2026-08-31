package ai.novaflow.observability.telemetry;

import ai.novaflow.common.telemetry.NovaFlowTracer;
import ai.novaflow.observability.config.NovaFlowTelemetryProperties;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Configuration
@EnableConfigurationProperties(NovaFlowTelemetryProperties.class)
public class NovaFlowTelemetryConfiguration {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "novaflow.telemetry", name = "enabled", havingValue = "true")
    public OpenTelemetrySdk openTelemetrySdk(NovaFlowTelemetryProperties properties) {
        String endpoint = resolveOtlpEndpoint(properties);
        var exporterBuilder = OtlpHttpSpanExporter.builder()
                .setEndpoint(endpoint);
        String authHeader = resolveAuthHeader(properties);
        if (StringUtils.hasText(authHeader)) {
            exporterBuilder.addHeader("Authorization", authHeader);
        }

        Resource resource = Resource.getDefault().toBuilder()
                .put(ResourceAttributes.SERVICE_NAME, properties.getServiceName())
                .build();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(exporterBuilder.build()).build())
                .setResource(resource)
                .build();

        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();
        log.info("OpenTelemetry enabled, exporting spans to {}", endpoint);
        return sdk;
    }

    @Bean
    @ConditionalOnProperty(prefix = "novaflow.telemetry", name = "enabled", havingValue = "true")
    public NovaFlowTracer novaFlowTracer(OpenTelemetry openTelemetry, NovaFlowTelemetryProperties properties) {
        Tracer tracer = openTelemetry.getTracer(properties.getServiceName());
        return new OpenTelemetryNovaFlowTracer(tracer);
    }

    private String resolveOtlpEndpoint(NovaFlowTelemetryProperties properties) {
        if (StringUtils.hasText(properties.getLangfusePublicKey())
                && StringUtils.hasText(properties.getLangfuseSecretKey())) {
            String host = properties.getLangfuseHost();
            if (!StringUtils.hasText(host)) {
                host = "https://cloud.langfuse.com";
            }
            return host.replaceAll("/$", "") + "/api/public/otel/v1/traces";
        }
        return properties.getOtlpEndpoint();
    }

    private String resolveAuthHeader(NovaFlowTelemetryProperties properties) {
        if (!StringUtils.hasText(properties.getLangfusePublicKey())
                || !StringUtils.hasText(properties.getLangfuseSecretKey())) {
            return null;
        }
        String token = properties.getLangfusePublicKey() + ":" + properties.getLangfuseSecretKey();
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    @Slf4j
    static class OpenTelemetryNovaFlowTracer implements NovaFlowTracer {

        private final Tracer tracer;

        OpenTelemetryNovaFlowTracer(Tracer tracer) {
            this.tracer = tracer;
        }

        @Override
        public SpanScope startSpan(String spanName, Map<String, String> attributes) {
            Span span = tracer.spanBuilder(spanName).startSpan();
            if (attributes != null) {
                attributes.forEach((key, value) -> {
                    if (StringUtils.hasText(key) && value != null) {
                        span.setAttribute(AttributeKey.stringKey(key), value);
                    }
                });
            }
            return new OtelSpanScope(span);
        }

        static class OtelSpanScope implements SpanScope {
            private final Span span;

            OtelSpanScope(Span span) {
                this.span = span;
            }

            @Override
            public void recordError(Throwable error) {
                if (error != null) {
                    span.recordException(error);
                    span.setStatus(StatusCode.ERROR, error.getMessage());
                }
            }

            @Override
            public void setAttribute(String key, String value) {
                if (StringUtils.hasText(key) && value != null) {
                    span.setAttribute(AttributeKey.stringKey(key), value);
                }
            }

            @Override
            public void close() {
                span.end();
            }
        }
    }

}