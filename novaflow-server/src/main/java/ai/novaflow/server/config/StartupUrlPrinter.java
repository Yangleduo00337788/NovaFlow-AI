package ai.novaflow.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 启动完成后在控制台打印 API 与 Web 访问地址。不在日志中输出任何账号凭据。
 */
@Slf4j
@Component
public class StartupUrlPrinter implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${novaflow.web.base-url:http://localhost:3000}")
    private String webBaseUrl;

    @Value("${novaflow.web.print-urls-on-startup:true}")
    private boolean printUrlsOnStartup;

    private final Environment environment;

    public StartupUrlPrinter(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!printUrlsOnStartup) {
            return;
        }

        String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "8080"));
        String contextPath = environment.getProperty("server.servlet.context-path", "");
        if (contextPath == null) {
            contextPath = "";
        }
        if (!contextPath.isEmpty() && !contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        if (contextPath.endsWith("/")) {
            contextPath = contextPath.substring(0, contextPath.length() - 1);
        }

        String apiBase = "http://localhost:" + port + contextPath;
        String web = trimTrailingSlash(webBaseUrl);

        String line = "─".repeat(56);
        log.info("""

                {}
                  NovaFlow AI 后端已启动 — 访问地址
                {}
                  API 服务          {}
                  健康检查          {}/actuator/health
                  Swagger UI        {}/swagger-ui.html

                  Web 门户          {}/login
                {}
                """,
                line,
                line,
                apiBase,
                apiBase,
                apiBase,
                web,
                line);
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:3000";
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
