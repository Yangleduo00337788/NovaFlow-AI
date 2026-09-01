package ai.novaflow.server.controller;

import ai.novaflow.common.domain.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public ApiResult<Map<String, String>> health() {
        return ApiResult.ok(Map.of(
                "status", "UP",
                "application", "NovaFlow AI",
                "version", "1.0.1"
        ));
    }
}
