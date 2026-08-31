package ai.novaflow.server.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 基于 Testcontainers 的集成测试基类。
 * Docker 不可用时自动跳过（Windows Docker Desktop 29 + Testcontainers 1.20 存在已知兼容问题）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractTestcontainersIntegrationTest extends IntegrationTestSupport {

    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.0");
    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

    @Container
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>(MYSQL_IMAGE)
            .withDatabaseName("novaflow")
            .withUsername("root")
            .withPassword("root");

    @Container
    protected static final GenericContainer<?> REDIS = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", "redis123");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> String.valueOf(REDIS.getMappedPort(6379)));
        registerCommonProperties(registry);
        registerRedisPassword(registry, "redis123");
    }

    static void registerCommonProperties(DynamicPropertyRegistry registry) {
        registry.add("novaflow.security.crypto-key", () -> "NovaFlowAI-TestKey-32bytes!!!!!");
        registry.add("novaflow.storage.access-key", () -> "test-access-key");
        registry.add("novaflow.storage.secret-key", () -> "test-secret-key");
        registry.add("novaflow.storage.endpoint", () -> "http://localhost:9000");
        registry.add("novaflow.qdrant.host", () -> "localhost");
        registry.add("novaflow.qdrant.port", () -> "6334");
    }

    static void registerRedisPassword(DynamicPropertyRegistry registry, String password) {
        registry.add("spring.data.redis.password", () -> password);
    }
}
