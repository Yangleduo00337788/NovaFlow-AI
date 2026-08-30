package ai.novaflow.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "novaflow.cors")
public class CorsProperties {

    /**
     * 允许的前端来源，生产环境请配置为实际域名。
     */
    private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:3000"));
}
