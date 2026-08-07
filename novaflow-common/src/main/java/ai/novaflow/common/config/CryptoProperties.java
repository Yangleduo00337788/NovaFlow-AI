package ai.novaflow.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "novaflow.security")
public class CryptoProperties {

    /**
     * AES 密钥（开发环境默认值，生产环境请通过环境变量覆盖）
     */
    private String cryptoKey = "NovaFlowAI-DevKey-32bytes!!!!";
}
