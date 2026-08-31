package ai.novaflow.observability.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "novaflow.telemetry")
public class NovaFlowTelemetryProperties {

    private boolean enabled = false;
    private String serviceName = "novaflow-server";
    private String otlpEndpoint = "http://localhost:4318";
    private String langfusePublicKey;
    private String langfuseSecretKey;
    private String langfuseHost = "https://cloud.langfuse.com";
}
