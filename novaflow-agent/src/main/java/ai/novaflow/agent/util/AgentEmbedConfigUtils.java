package ai.novaflow.agent.util;

import ai.novaflow.agent.domain.AgentEmbedConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
public final class AgentEmbedConfigUtils {

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private AgentEmbedConfigUtils() {
    }

    public static AgentEmbedConfig parse(ObjectMapper objectMapper, String json) {
        if (!StringUtils.hasText(json)) {
            return new AgentEmbedConfig();
        }
        try {
            AgentEmbedConfig config = objectMapper.readValue(json, AgentEmbedConfig.class);
            return normalize(config);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse embed_config JSON: {}", json, e);
            return new AgentEmbedConfig();
        }
    }

    public static String serialize(ObjectMapper objectMapper, AgentEmbedConfig config) {
        AgentEmbedConfig normalized = normalize(config);
        if (!StringUtils.hasText(normalized.getThemeColor())
                && (normalized.getAllowedDomains() == null || normalized.getAllowedDomains().isEmpty())
                && (!StringUtils.hasText(normalized.getPostMessageTargetOrigin())
                || "*".equals(normalized.getPostMessageTargetOrigin()))) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize embed_config", e);
            return null;
        }
    }

    public static AgentEmbedConfig normalize(AgentEmbedConfig config) {
        if (config == null) {
            return new AgentEmbedConfig();
        }
        AgentEmbedConfig normalized = new AgentEmbedConfig();
        normalized.setThemeColor(normalizeThemeColor(config.getThemeColor()));
        normalized.setAllowedDomains(normalizeDomains(config.getAllowedDomains()));
        normalized.setPostMessageTargetOrigin(normalizeTargetOrigin(config.getPostMessageTargetOrigin()));
        return normalized;
    }

    public static String normalizeThemeColor(String color) {
        if (!StringUtils.hasText(color)) {
            return null;
        }
        String trimmed = color.trim();
        if (!trimmed.startsWith("#")) {
            trimmed = "#" + trimmed;
        }
        return HEX_COLOR.matcher(trimmed).matches() ? trimmed.toLowerCase(Locale.ROOT) : null;
    }

    public static List<String> normalizeDomains(List<String> domains) {
        if (domains == null || domains.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> normalized = new ArrayList<>();
        for (String domain : domains) {
            if (!StringUtils.hasText(domain)) {
                continue;
            }
            String item = domain.trim().toLowerCase(Locale.ROOT);
            if (item.startsWith("http://") || item.startsWith("https://")) {
                try {
                    item = java.net.URI.create(item).getHost();
                } catch (Exception ignored) {
                    continue;
                }
            }
            item = item.replaceAll("/+$", "");
            if (item.startsWith("*.")) {
                item = "*." + item.substring(2).replaceAll("^\\*\\.", "");
            }
            if (StringUtils.hasText(item) && !normalized.contains(item)) {
                normalized.add(item);
            }
        }
        return normalized;
    }

    public static String normalizeTargetOrigin(String origin) {
        if (!StringUtils.hasText(origin)) {
            return "*";
        }
        String trimmed = origin.trim();
        return "*".equals(trimmed) ? "*" : trimmed;
    }
}
