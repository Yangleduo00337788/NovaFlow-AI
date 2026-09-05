package ai.novaflow.security.filter;

import ai.novaflow.common.security.AccountTypes;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 平台账号与租户账号 API 域隔离：平台账号仅可访问平台治理与认证接口。
 */
@Component
@Order(2)
public class PlatformAccessFilter extends OncePerRequestFilter {

    private static final List<String> SKIP_PREFIXES = List.of(
            "/api/v1/auth/",
            "/api/v1/health",
            "/api/v1/open/"
    );

    private static final List<String> PLATFORM_ALLOWED_PREFIXES = List.of(
            "/api/v1/platform/",
            "/api/v1/auth/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (shouldSkip(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!StpUtil.isLogin()) {
            filterChain.doFilter(request, response);
            return;
        }

        String accountType = (String) StpUtil.getSession().get("accountType");
        boolean platformAccount = AccountTypes.isPlatform(accountType);

        if (platformAccount && !isPlatformAllowed(path)) {
            writeForbidden(response, "平台账号无法访问企业租户资源");
            return;
        }
        if (!platformAccount && path.startsWith("/api/v1/platform/")) {
            writeForbidden(response, "需要平台管理员权限");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean shouldSkip(String path) {
        return SKIP_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private boolean isPlatformAllowed(String path) {
        return PLATFORM_ALLOWED_PREFIXES.stream().anyMatch(path::startsWith)
                || path.equals("/api/v1/health");
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"message\":\"" + message + "\"}");
    }
}
