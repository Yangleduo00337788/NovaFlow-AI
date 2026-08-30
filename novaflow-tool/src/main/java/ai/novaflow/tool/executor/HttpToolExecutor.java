package ai.novaflow.tool.executor;

import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.security.UrlSafetyValidator;
import ai.novaflow.tool.domain.HttpToolDefinition;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HttpToolExecutor {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([a-zA-Z0-9_]+)}}");
    private static final int MAX_RESPONSE_LENGTH = 8000;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public String execute(HttpToolDefinition tool, Map<String, Object> arguments) {
        if (tool == null || !StringUtils.hasText(tool.getUrl())) {
            throw new BusinessException("HTTP 工具 URL 未配置");
        }

        String method = StringUtils.hasText(tool.getMethod()) ? tool.getMethod().trim().toUpperCase() : "GET";
        String resolvedUrl = resolveTemplate(tool.getUrl(), arguments);
        UrlSafetyValidator.validateHttpUrl(resolvedUrl);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(resolvedUrl))
                .timeout(Duration.ofSeconds(30));

        if (tool.getHeaders() != null) {
            for (Map.Entry<String, String> entry : tool.getHeaders().entrySet()) {
                if (StringUtils.hasText(entry.getKey()) && entry.getValue() != null) {
                    builder.header(entry.getKey(), resolveTemplate(entry.getValue(), arguments));
                }
            }
        }

        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            String body = resolveRequestBody(tool, arguments);
            if (StringUtils.hasText(body)) {
                if (tool.getHeaders() == null || tool.getHeaders().keySet().stream()
                        .noneMatch(key -> "content-type".equalsIgnoreCase(key))) {
                    builder.header("Content-Type", "application/json");
                }
                builder.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }
        } else {
            builder.GET();
        }

        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String body = response.body() != null ? response.body() : "";
            if (body.length() > MAX_RESPONSE_LENGTH) {
                body = body.substring(0, MAX_RESPONSE_LENGTH) + "\n...(truncated)";
            }
            if (response.statusCode() >= 400) {
                return "HTTP " + response.statusCode() + ": " + body;
            }
            return body;
        } catch (Exception e) {
            throw new BusinessException("工具调用失败: " + e.getMessage());
        }
    }

    private String resolveTemplate(String template, Map<String, Object> arguments) {
        if (!StringUtils.hasText(template)) {
            return template;
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = arguments != null ? arguments.get(key) : null;
            String replacement = value != null
                    ? URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8)
                    : "";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String resolveRequestBody(HttpToolDefinition tool, Map<String, Object> arguments) {
        if (StringUtils.hasText(tool.getBodyTemplate())) {
            return resolveRawTemplate(tool.getBodyTemplate(), arguments);
        }
        if (arguments == null || arguments.isEmpty()) {
            return "";
        }
        return toJsonBody(arguments);
    }

    private String resolveRawTemplate(String template, Map<String, Object> arguments) {
        if (!StringUtils.hasText(template)) {
            return template;
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = arguments != null ? arguments.get(key) : null;
            String replacement = value != null ? String.valueOf(value) : "";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String toJsonBody(Map<String, Object> arguments) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append('"').append(escapeJson(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value instanceof Number || value instanceof Boolean) {
                builder.append(value);
            } else {
                builder.append('"').append(escapeJson(String.valueOf(value))).append('"');
            }
        }
        builder.append('}');
        return builder.toString();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
