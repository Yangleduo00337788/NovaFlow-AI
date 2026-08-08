package ai.novaflow.monitor.service;

import ai.novaflow.monitor.config.MonitorProperties;
import ai.novaflow.monitor.domain.vo.MonitorOverviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InfrastructureHealthChecker {

    private final DataSource dataSource;
    private final MonitorProperties monitorProperties;
    private final StringRedisTemplate redisTemplate;

    @Value("${novaflow.storage.endpoint:http://localhost:9000}")
    private String minioEndpoint;

    public List<MonitorOverviewVO.ServiceHealthVO> checkAll() {
        List<MonitorOverviewVO.ServiceHealthVO> services = new ArrayList<>();
        services.add(checkMysql());
        services.add(checkRedis());
        services.add(checkQdrant());
        services.add(checkMinio());
        services.add(apiHealth());
        return services;
    }

    private MonitorOverviewVO.ServiceHealthVO apiHealth() {
        return health("api", "API 服务", true, "运行中", "Spring Boot 应用正常");
    }

    private MonitorOverviewVO.ServiceHealthVO checkMysql() {
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2);
            return health("mysql", "MySQL", valid, valid ? "正常" : "异常", valid ? "连接可用" : "连接无效");
        } catch (Exception e) {
            log.debug("MySQL health check failed", e);
            return health("mysql", "MySQL", false, "异常", rootMessage(e));
        }
    }

    private MonitorOverviewVO.ServiceHealthVO checkRedis() {
        try {
            String pong = redisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
            boolean healthy = pong != null && !pong.isBlank();
            return health("redis", "Redis", healthy, healthy ? "正常" : "异常", healthy ? "PING 成功" : "无响应");
        } catch (Exception e) {
            log.debug("Redis health check failed", e);
            return health("redis", "Redis", false, "异常", rootMessage(e));
        }
    }

    private MonitorOverviewVO.ServiceHealthVO checkQdrant() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(monitorProperties.getHealthTimeoutMs()))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(monitorProperties.getQdrantHealthUrl()))
                    .timeout(Duration.ofMillis(monitorProperties.getHealthTimeoutMs()))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            boolean healthy = response.statusCode() >= 200 && response.statusCode() < 300;
            return health("qdrant", "Qdrant", healthy, healthy ? "正常" : "异常",
                    healthy ? "向量库就绪" : "HTTP " + response.statusCode());
        } catch (Exception e) {
            log.debug("Qdrant health check failed", e);
            return health("qdrant", "Qdrant", false, "异常", rootMessage(e));
        }
    }

    private MonitorOverviewVO.ServiceHealthVO checkMinio() {
        try {
            String base = minioEndpoint.endsWith("/") ? minioEndpoint.substring(0, minioEndpoint.length() - 1) : minioEndpoint;
            URI uri = URI.create(base + "/minio/health/live");
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(monitorProperties.getHealthTimeoutMs()))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofMillis(monitorProperties.getHealthTimeoutMs()))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            boolean healthy = response.statusCode() >= 200 && response.statusCode() < 300;
            return health("minio", "MinIO", healthy, healthy ? "正常" : "异常",
                    healthy ? "对象存储就绪" : "HTTP " + response.statusCode());
        } catch (Exception e) {
            log.debug("MinIO health check failed", e);
            return health("minio", "MinIO", false, "异常", rootMessage(e));
        }
    }

    private MonitorOverviewVO.ServiceHealthVO health(
            String key,
            String name,
            boolean healthy,
            String status,
            String detail) {
        return MonitorOverviewVO.ServiceHealthVO.builder()
                .key(key)
                .name(name)
                .healthy(healthy)
                .status(status)
                .detail(detail)
                .build();
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return "连接失败";
        }
        return message.length() > 80 ? message.substring(0, 80) + "..." : message;
    }
}
