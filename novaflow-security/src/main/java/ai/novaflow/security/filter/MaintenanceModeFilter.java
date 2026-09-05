package ai.novaflow.security.filter;

import ai.novaflow.common.security.AccountTypes;
import ai.novaflow.common.security.MaintenanceModeChecker;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 平台维护模式：租户侧 API 与 Open API 拦截；平台治理与公开状态接口放行。
 */
@Component
@Order(3)
public class MaintenanceModeFilter extends OncePerRequestFilter {

    private static final List<String> ALWAYS_ALLOW_PREFIXES = List.of(
            "/api/v1/public/",
            "/api/v1/health",
            "/api/v1/platform/",
            "/actuator/"
    );

    private final MaintenanceModeChecker maintenanceModeChecker;

    public MaintenanceModeFilter(@Autowired(required = false) MaintenanceModeChecker maintenanceModeChecker) {
        this.maintenanceModeChecker = maintenanceModeChecker;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (maintenanceModeChecker == null || !maintenanceModeChecker.isMaintenanceEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (isAlwaysAllowed(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        if ("/api/v1/auth/register".equals(path)) {
            writeMaintenance(response);
            return;
        }

        if ("/api/v1/auth/logout".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (StpUtil.isLogin()) {
            String accountType = (String) StpUtil.getSession().get("accountType");
            if (AccountTypes.isPlatform(accountType)) {
                filterChain.doFilter(request, response);
                return;
            }
            writeMaintenance(response);
            return;
        }

        if (path.startsWith("/api/v1/open/") || path.startsWith("/api/v1/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/api/")) {
            writeMaintenance(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAlwaysAllowed(String path) {
        return ALWAYS_ALLOW_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private void writeMaintenance(HttpServletResponse response) throws IOException {
        String message = maintenanceModeChecker.getMaintenanceMessage()
                .replace("\"", "\\\"")
                .replace("\n", " ");
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"code\":" + MaintenanceModeChecker.MAINTENANCE_CODE
                        + ",\"message\":\"" + message + "\"}");
    }
}
