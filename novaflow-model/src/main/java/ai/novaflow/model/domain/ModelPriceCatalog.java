package ai.novaflow.model.domain;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Getter
public final class ModelPriceCatalog {

    private ModelPriceCatalog() {
    }

    public record ModelPrice(BigDecimal inputPer1k, BigDecimal outputPer1k, BillingCurrency currency) {
    }

    private record KeywordPrice(String keyword, ModelPrice price) {
    }

    /** DeepSeek 官方人民币参考价（元 / 1K tokens） */
    private static final ModelPrice DEEPSEEK_DEFAULT = price("0.001", "0.002", BillingCurrency.CNY);
    /** OpenAI 美元参考价（USD / 1K tokens） */
    private static final ModelPrice OPENAI_DEFAULT = price("0.00015", "0.00060", BillingCurrency.USD);
    /** 硅基流动通用对话模型参考价（元 / 1K tokens） */
    private static final ModelPrice SILICONFLOW_CHAT_DEFAULT = price("0.001", "0.002", BillingCurrency.CNY);
    /** Embedding 模型参考价（元 / 1K tokens，输出按 0 计） */
    private static final ModelPrice EMBEDDING_DEFAULT = price("0.00007", "0", BillingCurrency.CNY);
    /** Rerank 模型参考价（元 / 1K tokens，输出按 0 计） */
    private static final ModelPrice RERANK_DEFAULT = price("0.00007", "0", BillingCurrency.CNY);

    private static final Map<String, ModelPrice> BY_MODEL = Map.ofEntries(
            Map.entry("deepseek-chat", DEEPSEEK_DEFAULT),
            Map.entry("deepseek-reasoner", price("0.004", "0.016", BillingCurrency.CNY)),
            Map.entry("deepseek-v3", price("0.002", "0.008", BillingCurrency.CNY)),
            Map.entry("deepseek-v3.1", price("0.002", "0.008", BillingCurrency.CNY)),
            Map.entry("deepseek-v3.2", price("0.002", "0.004", BillingCurrency.CNY)),
            Map.entry("deepseek-v4-flash", price("0.00013", "0.00028", BillingCurrency.CNY)),
            Map.entry("deepseek-v4-pro", price("0.00162", "0.00315", BillingCurrency.CNY)),
            Map.entry("deepseek-r1", price("0.004", "0.016", BillingCurrency.CNY)),
            Map.entry("gpt-4o", price("0.00250", "0.01000", BillingCurrency.USD)),
            Map.entry("gpt-4o-mini", price("0.00015", "0.00060", BillingCurrency.USD)),
            Map.entry("gpt-4.1", price("0.00200", "0.00800", BillingCurrency.USD)),
            Map.entry("gpt-4.1-mini", price("0.00040", "0.00160", BillingCurrency.USD)),
            Map.entry("gpt-3.5-turbo", price("0.00050", "0.00150", BillingCurrency.USD)),
            Map.entry("bge-m3", EMBEDDING_DEFAULT),
            Map.entry("bge-large-zh-v1.5", EMBEDDING_DEFAULT),
            Map.entry("bge-large-en-v1.5", EMBEDDING_DEFAULT),
            Map.entry("text-embedding-3-small", price("0.00002", "0", BillingCurrency.USD)),
            Map.entry("text-embedding-3-large", price("0.00013", "0", BillingCurrency.USD)),
            Map.entry("text-embedding-v3", price("0.00007", "0", BillingCurrency.CNY))
    );

    private static final List<KeywordPrice> BY_KEYWORD = List.of(
            new KeywordPrice("deepseek-v4-pro", BY_MODEL.get("deepseek-v4-pro")),
            new KeywordPrice("deepseek-v4-flash", BY_MODEL.get("deepseek-v4-flash")),
            new KeywordPrice("deepseek-v3.2", BY_MODEL.get("deepseek-v3.2")),
            new KeywordPrice("deepseek-v3.1", BY_MODEL.get("deepseek-v3.1")),
            new KeywordPrice("deepseek-v3", BY_MODEL.get("deepseek-v3")),
            new KeywordPrice("deepseek-reasoner", BY_MODEL.get("deepseek-reasoner")),
            new KeywordPrice("deepseek-r1", BY_MODEL.get("deepseek-r1")),
            new KeywordPrice("deepseek-chat", DEEPSEEK_DEFAULT),
            new KeywordPrice("bge-m3", EMBEDDING_DEFAULT),
            new KeywordPrice("bge-large", EMBEDDING_DEFAULT),
            new KeywordPrice("embed", EMBEDDING_DEFAULT),
            new KeywordPrice("rerank", RERANK_DEFAULT),
            new KeywordPrice("qwen3", price("0.0008", "0.002", BillingCurrency.CNY)),
            new KeywordPrice("qwen2.5", price("0.0008", "0.002", BillingCurrency.CNY)),
            new KeywordPrice("qwen2", price("0.0008", "0.002", BillingCurrency.CNY)),
            new KeywordPrice("glm-4", price("0.001", "0.001", BillingCurrency.CNY)),
            new KeywordPrice("glm-3", price("0.0005", "0.0005", BillingCurrency.CNY)),
            new KeywordPrice("gpt-4o-mini", BY_MODEL.get("gpt-4o-mini")),
            new KeywordPrice("gpt-4o", BY_MODEL.get("gpt-4o")),
            new KeywordPrice("gpt-4.1", BY_MODEL.get("gpt-4.1")),
            new KeywordPrice("gpt-3.5", BY_MODEL.get("gpt-3.5-turbo"))
    );

    private static final Map<String, ModelPrice> BY_PROVIDER = Map.of(
            "deepseek", DEEPSEEK_DEFAULT,
            "openai", OPENAI_DEFAULT,
            "siliconflow", SILICONFLOW_CHAT_DEFAULT,
            "qwen", price("0.0008", "0.002", BillingCurrency.CNY),
            "zhipu", price("0.001", "0.001", BillingCurrency.CNY),
            "moonshot", price("0.012", "0.012", BillingCurrency.CNY),
            "baichuan", price("0.001", "0.001", BillingCurrency.CNY),
            "minimax", price("0.001", "0.001", BillingCurrency.CNY)
    );

    public static Optional<ModelPrice> resolve(String providerCode, String modelName) {
        String normalized = normalizeModelName(modelName);
        if (!normalized.isEmpty()) {
            ModelPrice exact = BY_MODEL.get(normalized);
            if (exact != null) {
                return Optional.of(exact);
            }
            for (Map.Entry<String, ModelPrice> entry : BY_MODEL.entrySet()) {
                if (normalized.startsWith(entry.getKey())) {
                    return Optional.of(entry.getValue());
                }
            }
            for (KeywordPrice keywordPrice : BY_KEYWORD) {
                if (normalized.contains(keywordPrice.keyword())) {
                    return Optional.of(keywordPrice.price());
                }
            }
            if (normalized.contains("embed")) {
                return Optional.of(EMBEDDING_DEFAULT);
            }
            if (normalized.contains("rerank")) {
                return Optional.of(RERANK_DEFAULT);
            }
        }
        if (providerCode != null) {
            ModelPrice providerPrice = BY_PROVIDER.get(providerCode.trim().toLowerCase(Locale.ROOT));
            if (providerPrice != null) {
                return Optional.of(providerPrice);
            }
        }
        return Optional.empty();
    }

    public static String normalizeModelName(String modelName) {
        if (modelName == null) {
            return "";
        }
        String normalized = modelName.trim().toLowerCase(Locale.ROOT);
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0 && slash < normalized.length() - 1) {
            normalized = normalized.substring(slash + 1);
        }
        return normalized;
    }

    private static ModelPrice price(String inputPer1k, String outputPer1k, BillingCurrency currency) {
        return new ModelPrice(new BigDecimal(inputPer1k), new BigDecimal(outputPer1k), currency);
    }
}
