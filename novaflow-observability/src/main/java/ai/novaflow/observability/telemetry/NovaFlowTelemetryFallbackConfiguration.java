package ai.novaflow.observability.telemetry;

import ai.novaflow.common.telemetry.NoopNovaFlowTracer;
import ai.novaflow.common.telemetry.NovaFlowTracer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NovaFlowTelemetryFallbackConfiguration {

    @Bean
    @ConditionalOnMissingBean(NovaFlowTracer.class)
    public NovaFlowTracer noopNovaFlowTracer() {
        return NoopNovaFlowTracer.INSTANCE;
    }
}
