package ai.novaflow.common.security;

import ai.novaflow.common.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * SSRF 防护：解析 URL 后校验目标主机，禁止访问内网、回环与链路本地地址。
 */
public final class UrlSafetyValidator {

    private UrlSafetyValidator() {
    }

    public static void validateHttpUrl(String url) {
        if (!StringUtils.hasText(url)) {
            throw new BusinessException("URL 不能为空");
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("URL 格式无效");
        }

        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new BusinessException("工具 URL 必须使用 http/https 协议");
        }
        String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
        if (!"http".equals(normalizedScheme) && !"https".equals(normalizedScheme)) {
            throw new BusinessException("工具 URL 必须使用 http/https 协议");
        }

        String host = uri.getHost();
        if (!StringUtils.hasText(host)) {
            throw new BusinessException("URL 主机无效");
        }

        String lowerHost = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(lowerHost)
                || lowerHost.endsWith(".localhost")
                || "0.0.0.0".equals(lowerHost)) {
            throw new BusinessException("工具 URL 不允许访问本地地址");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isBlockedAddress(address)) {
                    throw new BusinessException("工具 URL 不允许访问内网或保留地址");
                }
            }
        } catch (UnknownHostException ex) {
            throw new BusinessException("无法解析 URL 主机: " + host);
        }
    }

    private static boolean isBlockedAddress(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || isUniqueLocalIpv6(address);
    }

    /** IPv6 ULA (fc00::/7) 不被 InetAddress.isSiteLocalAddress 视为内网。 */
    private static boolean isUniqueLocalIpv6(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes != null && bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC;
    }
}
