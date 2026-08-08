package ai.novaflow.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "novaflow.qdrant")
public class QdrantProperties {

    private String host = "localhost";
    private int port = 6334;
    private boolean useTls = false;
    /** 可选；本地 Docker 默认不启用 API Key */
    private String apiKey = "";
}
