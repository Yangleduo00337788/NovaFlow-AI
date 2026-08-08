package ai.novaflow.prompt.util;

import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PromptRenderUtils {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([a-zA-Z0-9_]+)}}");

    private PromptRenderUtils() {
    }

    public static String render(String template, Map<String, Object> variables) {
        if (!StringUtils.hasText(template)) {
            return template;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = variables != null ? variables.get(key) : null;
            String replacement = value != null ? String.valueOf(value) : "";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
