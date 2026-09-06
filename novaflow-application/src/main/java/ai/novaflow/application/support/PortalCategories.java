package ai.novaflow.application.support;

import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PortalCategories {

    public static final String DEFAULT_CODE = "general";

    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        LABELS.put("general", "通用");
        LABELS.put("office", "办公效率");
        LABELS.put("customer_service", "客户服务");
        LABELS.put("knowledge", "知识问答");
        LABELS.put("writing", "内容创作");
    }

    private PortalCategories() {
    }

    public static String normalize(String raw) {
        if (!StringUtils.hasText(raw)) {
            return DEFAULT_CODE;
        }
        String normalized = raw.trim().toLowerCase();
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }

    public static String labelOf(String code) {
        String normalized = normalize(code);
        return LABELS.getOrDefault(normalized, normalized);
    }

    public static Map<String, String> predefined() {
        return LABELS;
    }
}
