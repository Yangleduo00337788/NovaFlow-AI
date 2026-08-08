package ai.novaflow.model.domain;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModelExtraParametersBuilder {

    private ModelExtraParametersBuilder() {
    }

    public static Map<String, Object> build(ResolvedModelConfig config) {
        if (config == null) {
            return Map.of();
        }
        Map<String, Object> extra = new LinkedHashMap<>();
        String provider = normalize(config.getProviderCode());
        String model = normalize(config.getModelName());

        if (Boolean.TRUE.equals(config.getEnableDeepThinking())) {
            appendDeepThinking(extra, provider, model);
        }
        if (Boolean.TRUE.equals(config.getEnableWebSearch())) {
            appendWebSearch(extra, provider, model);
        }
        return extra;
    }

    private static void appendDeepThinking(Map<String, Object> extra, String provider, String model) {
        if ("qwen".equals(provider) || model.contains("qwen")) {
            extra.put("enable_thinking", true);
            extra.put("incremental_output", true);
            return;
        }
        if ("deepseek".equals(provider) || model.contains("deepseek")) {
            extra.put("thinking", Map.of("type", "enabled"));
            return;
        }
        if ("moonshot".equals(provider) || model.contains("kimi")) {
            extra.put("thinking", Map.of("type", "enabled"));
            return;
        }
        if ("zhipu".equals(provider) || model.contains("glm")) {
            extra.put("thinking", Map.of("type", "enabled"));
        }
    }

    private static void appendWebSearch(Map<String, Object> extra, String provider, String model) {
        if ("qwen".equals(provider) || model.contains("qwen")) {
            extra.put("enable_search", true);
            extra.put("search_options", Map.of("search_strategy", "agent"));
            return;
        }
        if ("moonshot".equals(provider) || model.contains("kimi")) {
            extra.put("use_search", true);
            return;
        }
        if ("zhipu".equals(provider) || model.contains("glm")) {
            extra.put("tools", new Object[]{
                    Map.of("type", "web_search", "web_search", Map.of("enable", true))
            });
            return;
        }
        if ("baidu".equals(provider)) {
            extra.put("web_search", Map.of("enable", true));
        }
    }

    private static String normalize(String value) {
        return value != null ? value.trim().toLowerCase() : "";
    }
}
