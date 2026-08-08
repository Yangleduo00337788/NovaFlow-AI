package ai.novaflow.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "novaflow.storage")
public class StorageProperties {

    private String endpoint = "http://localhost:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin123";
    private String bucket = "novaflow";
}
