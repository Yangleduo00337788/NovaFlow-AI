package ai.novaflow.server.config;

import ai.novaflow.common.config.CorsProperties;
import ai.novaflow.common.config.CryptoProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

import java.util.List;

@Configuration
@Profile("prod")
@RequiredArgsConstructor
public class ProdSecurityValidator {

    private final CryptoProperties cryptoProperties;
    private final CorsProperties corsProperties;

    @PostConstruct
    public void validate() {
        String key = cryptoProperties.getCryptoKey();
        if (!StringUtils.hasText(key) || key.length() < 32 || isWeakCryptoKey(key)) {
            throw new IllegalStateException(
                    "生产环境必须设置强随机 NOVAFLOW_CRYPTO_KEY（至少 32 字符，禁止使用示例/占位值）");
        }
        validateCorsOrigins(corsProperties.getAllowedOrigins(), corsProperties.isAllowLocalhost());
    }

    static boolean isWeakCryptoKey(String key) {
        String normalized = key.toLowerCase();
        return normalized.contains("请替换")
                || normalized.contains("change-me")
                || normalized.contains("changeme")
                || normalized.contains("example")
                || normalized.contains("crypto-key-here");
    }

    static void validateCorsOrigins(List<String> origins, boolean allowLocalhost) {
        if (origins == null || origins.isEmpty()) {
            throw new IllegalStateException("生产环境必须设置 CORS_ALLOWED_ORIGIN 为真实前端域名");
        }
        for (String origin : origins) {
            if (!StringUtils.hasText(origin) || "*".equals(origin.trim())) {
                throw new IllegalStateException("生产 CORS 禁止空值或 *，请设置 CORS_ALLOWED_ORIGIN");
            }
            if (!allowLocalhost && isLocalOrigin(origin)) {
                throw new IllegalStateException(
                        "生产 CORS 禁止 localhost / 127.0.0.1。请将 CORS_ALLOWED_ORIGIN 设为真实前端域名；本机冒烟可设 NOVAFLOW_CORS_ALLOW_LOCALHOST=true");
            }
        }
    }

    static boolean isLocalOrigin(String origin) {
        String normalized = origin.trim().toLowerCase();
        return normalized.contains("localhost") || normalized.contains("127.0.0.1") || normalized.contains("[::1]");
    }
}
