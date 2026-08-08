package ai.novaflow.monitor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "novaflow.monitor")
public class MonitorProperties {

    private String qdrantHealthUrl = "http://localhost:6333/readyz";
    private int healthTimeoutMs = 3000;
}
