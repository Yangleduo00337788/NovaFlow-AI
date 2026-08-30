package ai.novaflow.common.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Data
@Component
@ConfigurationProperties(prefix = "novaflow.security")
public class CryptoProperties {

    private String cryptoKey;

    @PostConstruct
    void validate() {
        if (!StringUtils.hasText(cryptoKey)) {
            throw new IllegalStateException(
                    "缺少加密密钥：请设置环境变量 NOVAFLOW_CRYPTO_KEY（参考 .env.example）");
        }
    }
}
