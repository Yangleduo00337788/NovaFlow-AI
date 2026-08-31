package ai.novaflow.server.config;

import ai.novaflow.common.config.CryptoProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

@Configuration
@Profile("prod")
@RequiredArgsConstructor
public class ProdSecurityValidator {

    private final CryptoProperties cryptoProperties;

    @PostConstruct
    public void validate() {
        String key = cryptoProperties.getCryptoKey();
        if (!StringUtils.hasText(key) || key.length() < 32 || key.contains("请替换")) {
            throw new IllegalStateException(
                    "生产环境必须设置 NOVAFLOW_CRYPTO_KEY（至少 32 字符随机字符串）");
        }
    }
}
