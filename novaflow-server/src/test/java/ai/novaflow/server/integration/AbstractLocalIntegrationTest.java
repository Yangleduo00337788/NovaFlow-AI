package ai.novaflow.server.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 使用本机 MySQL + Redis 的集成测试基类（不依赖 Testcontainers Java 客户端）。
 * 配置读取顺序：系统环境变量 &gt; 项目根目录 {@code .env} &gt; 内置默认值。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractLocalIntegrationTest extends IntegrationTestSupport {

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> IntegrationTestEnv.get(
                "SPRING_DATASOURCE_URL",
                "jdbc:mysql://localhost:3306/novaflow?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
        ));
        registry.add("spring.datasource.username", () -> IntegrationTestEnv.get("SPRING_DATASOURCE_USERNAME", "root"));
        registry.add("spring.datasource.password", () -> IntegrationTestEnv.get("SPRING_DATASOURCE_PASSWORD", "root"));
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.data.redis.host", () -> IntegrationTestEnv.get("REDIS_HOST", "localhost"));
        registry.add("spring.data.redis.port", () -> IntegrationTestEnv.get("REDIS_PORT", "6379"));
        AbstractTestcontainersIntegrationTest.registerCommonProperties(registry);
        AbstractTestcontainersIntegrationTest.registerRedisPassword(
                registry,
                IntegrationTestEnv.get("REDIS_PASSWORD", "redis123")
        );
        registry.add("novaflow.security.crypto-key", () -> IntegrationTestEnv.get(
                "NOVAFLOW_CRYPTO_KEY",
                "NovaFlowAI-DevKey-32bytes!!!!"
        ));
        registry.add("novaflow.storage.endpoint", () -> IntegrationTestEnv.get("MINIO_ENDPOINT", "http://localhost:9000"));
        registry.add("novaflow.storage.access-key", () -> IntegrationTestEnv.get("MINIO_ACCESS_KEY", "test-access-key"));
        registry.add("novaflow.storage.secret-key", () -> IntegrationTestEnv.get("MINIO_SECRET_KEY", "test-secret-key"));
        registry.add("novaflow.storage.bucket", () -> IntegrationTestEnv.get("MINIO_BUCKET", "novaflow"));
        registry.add("novaflow.qdrant.host", () -> IntegrationTestEnv.get("QDRANT_HOST", "localhost"));
        registry.add("novaflow.qdrant.port", () -> IntegrationTestEnv.get("QDRANT_PORT", "6334"));
        registry.add("novaflow.qdrant.api-key", () -> IntegrationTestEnv.get("QDRANT_API_KEY", ""));
    }
}
