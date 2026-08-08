package ai.novaflow.model.domain;

import org.springframework.util.StringUtils;

public final class ModelCapabilityResolver {

    private ModelCapabilityResolver() {
    }

    public static boolean supportsDeepThinking(String providerCode, String modelName) {
        String provider = normalize(providerCode);
        String model = normalize(modelName);
        if (!StringUtils.hasText(model)) {
            return false;
        }
        if (model.contains("reasoner")
                || model.contains("-r1")
                || model.contains("thinking")
                || model.startsWith("o1")
                || model.startsWith("o3")
                || model.startsWith("o4")) {
            return true;
        }
        return switch (provider) {
            case "qwen", "deepseek", "moonshot", "zhipu", "baidu", "doubao", "minimax" -> true;
            default -> model.contains("qwen") || model.contains("glm") || model.contains("kimi");
        };
    }

    public static boolean supportsWebSearch(String providerCode, String modelName) {
        String provider = normalize(providerCode);
        String model = normalize(modelName);
        if (!StringUtils.hasText(model)) {
            return false;
        }
        if (model.contains("search") || model.contains("browse")) {
            return true;
        }
        return switch (provider) {
            case "qwen", "moonshot", "zhipu", "baidu", "doubao" -> true;
            default -> model.contains("qwen") || model.contains("glm") || model.contains("kimi");
        };
    }

    private static String normalize(String value) {
        return value != null ? value.trim().toLowerCase() : "";
    }
}
