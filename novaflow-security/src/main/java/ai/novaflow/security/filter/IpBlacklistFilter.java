package ai.novaflow.security.filter;

import ai.novaflow.common.security.IpBlacklistChecker;
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
 * 平台 IP 黑名单：拦截登录与 API 请求。
 */
@Component
@Order(1)
public class IpBlacklistFilter extends OncePerRequestFilter {

    private static final List<String> SKIP_PREFIXES = List.of(
            "/actuator/"
    );

    private final IpBlacklistChecker ipBlacklistChecker;

    public IpBlacklistFilter(@Autowired(required = false) IpBlacklistChecker ipBlacklistChecker) {
        this.ipBlacklistChecker = ipBlacklistChecker;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (shouldSkip(path) || ipBlacklistChecker == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = request.getRemoteAddr();
        if (ipBlacklistChecker.isBlocked(clientIp)) {
            writeForbidden(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean shouldSkip(String path) {
        return SKIP_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private void writeForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":40301,\"message\":\"IP 已被禁止访问\"}");
    }
}
