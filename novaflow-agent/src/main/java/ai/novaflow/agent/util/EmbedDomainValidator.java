package ai.novaflow.agent.util;

import ai.novaflow.agent.domain.AgentEmbedConfig;
import ai.novaflow.common.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.List;
import java.util.Locale;

public final class EmbedDomainValidator {

    private static final int EMBED_DOMAIN_FORBIDDEN = 40304;

    private EmbedDomainValidator() {
    }

    public static void requireAllowed(AgentEmbedConfig config, String referer, String origin) {
        if (config == null || config.getAllowedDomains() == null || config.getAllowedDomains().isEmpty()) {
            return;
        }
        String host = extractHost(referer);
        if (!StringUtils.hasText(host)) {
            host = extractHost(origin);
        }
        if (!StringUtils.hasText(host)) {
            return;
        }
        if (!matchesAny(host, config.getAllowedDomains())) {
            throw new BusinessException(EMBED_DOMAIN_FORBIDDEN, "当前来源域名不在 Embed 白名单中");
        }
    }

    static String extractHost(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost();
            if (StringUtils.hasText(host)) {
                return host.toLowerCase(Locale.ROOT);
            }
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }

    static boolean matchesAny(String host, List<String> allowedDomains) {
        for (String allowed : allowedDomains) {
            if (!StringUtils.hasText(allowed)) {
                continue;
            }
            if (matches(host, allowed.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    static boolean matches(String host, String pattern) {
        if (host.equals(pattern)) {
            return true;
        }
        if (pattern.startsWith("*.")) {
            String suffix = pattern.substring(1);
            return host.endsWith(suffix) || host.equals(pattern.substring(2));
        }
        return host.endsWith("." + pattern);
    }
}
