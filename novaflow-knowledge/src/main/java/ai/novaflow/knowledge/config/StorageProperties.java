package ai.novaflow.knowledge.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Data
@ConfigurationProperties(prefix = "novaflow.storage")
public class StorageProperties {

    private String endpoint = "http://localhost:9000";
    private String accessKey;
    private String secretKey;
    private String bucket = "novaflow";

    @PostConstruct
    void validate() {
        if (!StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)) {
            throw new IllegalStateException(
                    "缺少 MinIO 凭证：请设置环境变量 MINIO_ACCESS_KEY 与 MINIO_SECRET_KEY（参考 .env.example）");
        }
    }
}
