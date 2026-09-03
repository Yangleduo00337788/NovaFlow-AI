package ai.novaflow.server.controller;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.monitor.domain.vo.MonitorOverviewVO;
import ai.novaflow.monitor.service.InfrastructureHealthChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HealthController {

    private static final Instant STARTED_AT = Instant.now();

    private final InfrastructureHealthChecker infrastructureHealthChecker;

    @GetMapping("/health")
    public ResponseEntity<ApiResult<Map<String, Object>>> health() {
        List<MonitorOverviewVO.ServiceHealthVO> services;
        try {
            services = infrastructureHealthChecker.checkAll();
        } catch (Exception e) {
            Map<String, Object> body = identity("DOWN");
            body.put("checks", Map.of("self", Map.of(
                    "healthy", false,
                    "status", "异常",
                    "detail", "健康检查执行失败"
            )));
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiResult<>(50300, "核心依赖不可用", body, System.currentTimeMillis()));
        }
        boolean coreUp = services.stream()
                .filter(service -> "mysql".equals(service.getKey()) || "redis".equals(service.getKey()))
                .allMatch(MonitorOverviewVO.ServiceHealthVO::isHealthy);

        Map<String, Object> checks = new LinkedHashMap<>();
        for (MonitorOverviewVO.ServiceHealthVO service : services) {
            if ("api".equals(service.getKey())) {
                continue;
            }
            checks.put(service.getKey(), Map.of(
                    "healthy", service.isHealthy(),
                    "status", service.getStatus(),
                    "detail", service.getDetail() != null ? service.getDetail() : ""
            ));
        }

        Map<String, Object> body = identity(coreUp ? "UP" : "DOWN");
        body.put("checks", checks);

        ApiResult<Map<String, Object>> result = coreUp
                ? ApiResult.ok(body)
                : new ApiResult<>(50300, "核心依赖不可用", body, System.currentTimeMillis());
        return ResponseEntity.status(coreUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(result);
    }

    private static Map<String, Object> identity(String status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("application", "NovaFlow AI");
        body.put("version", "1.0.1");
        body.put("startedAt", STARTED_AT.toString());
        return body;
    }
}
