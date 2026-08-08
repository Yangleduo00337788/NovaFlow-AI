package ai.novaflow.rag.retrieval;

import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 轻量关键词匹配打分，用于向量召回后的混合重排（无需额外搜索引擎）。
 */
public final class KeywordMatchScorer {

    private static final Pattern TOKEN_SPLITTER = Pattern.compile("[\\s\\p{Punct}]+");

    private KeywordMatchScorer() {
    }

    public static float score(String query, String text) {
        if (!StringUtils.hasText(query) || !StringUtils.hasText(text)) {
            return 0F;
        }
        Set<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) {
            return 0F;
        }
        String normalizedText = text.toLowerCase(Locale.ROOT);
        int matched = 0;
        for (String token : queryTokens) {
            if (normalizedText.contains(token)) {
                matched++;
            }
        }
        return (float) matched / queryTokens.size();
    }

    private static Set<String> tokenize(String value) {
        Set<String> tokens = new HashSet<>();
        String normalized = value.toLowerCase(Locale.ROOT).trim();
        if (normalized.isEmpty()) {
            return tokens;
        }
        for (String part : TOKEN_SPLITTER.split(normalized)) {
            if (part.length() >= 2) {
                tokens.add(part);
            }
        }
        if (tokens.isEmpty() && normalized.length() >= 2) {
            tokens.add(normalized);
        }
        return tokens;
    }
}
