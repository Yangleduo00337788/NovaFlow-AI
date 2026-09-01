package ai.novaflow.server.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 使用本机 MySQL + Docker Redis 的集成测试基类（不依赖 Testcontainers Java 客户端）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractLocalIntegrationTest extends IntegrationTestSupport {

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> env(
                "SPRING_DATASOURCE_URL",
                "jdbc:mysql://localhost:3306/novaflow?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
        ));
        registry.add("spring.datasource.username", () -> env("SPRING_DATASOURCE_USERNAME", "root"));
        registry.add("spring.datasource.password", () -> env("SPRING_DATASOURCE_PASSWORD", "root"));
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.data.redis.host", () -> env("REDIS_HOST", "localhost"));
        registry.add("spring.data.redis.port", () -> env("REDIS_PORT", "6379"));
        AbstractTestcontainersIntegrationTest.registerCommonProperties(registry);
        AbstractTestcontainersIntegrationTest.registerRedisPassword(registry, env("REDIS_PASSWORD", "redis123"));
        registry.add("novaflow.security.crypto-key", () -> env(
                "NOVAFLOW_CRYPTO_KEY",
                "NovaFlowAI-DevKey-32bytes!!!!"
        ));
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null && !value.isBlank() ? value : defaultValue;
    }
}
