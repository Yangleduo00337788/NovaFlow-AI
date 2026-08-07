package ai.novaflow.model.domain;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Getter
public final class ModelPriceCatalog {

    private ModelPriceCatalog() {
    }

    public record ModelPrice(BigDecimal inputPer1k, BigDecimal outputPer1k, BillingCurrency currency) {
    }

    /** DeepSeek 官方人民币参考价（元 / 1K tokens） */
    private static final ModelPrice DEEPSEEK_DEFAULT = price("0.001", "0.002", BillingCurrency.CNY);
    /** OpenAI 美元参考价（USD / 1K tokens） */
    private static final ModelPrice OPENAI_DEFAULT = price("0.00015", "0.00060", BillingCurrency.USD);

    private static final Map<String, ModelPrice> BY_MODEL = Map.ofEntries(
            Map.entry("deepseek-chat", DEEPSEEK_DEFAULT),
            Map.entry("deepseek-reasoner", price("0.004", "0.016", BillingCurrency.CNY)),
            Map.entry("deepseek-v3", price("0.002", "0.008", BillingCurrency.CNY)),
            Map.entry("deepseek-v3.1", price("0.002", "0.008", BillingCurrency.CNY)),
            Map.entry("deepseek-v4-flash", price("0.0005", "0.002", BillingCurrency.CNY)),
            Map.entry("deepseek-v4-pro", price("0.002", "0.008", BillingCurrency.CNY)),
            Map.entry("gpt-4o", price("0.00250", "0.01000", BillingCurrency.USD)),
            Map.entry("gpt-4o-mini", price("0.00015", "0.00060", BillingCurrency.USD)),
            Map.entry("gpt-4.1", price("0.00200", "0.00800", BillingCurrency.USD)),
            Map.entry("gpt-4.1-mini", price("0.00040", "0.00160", BillingCurrency.USD)),
            Map.entry("gpt-3.5-turbo", price("0.00050", "0.00150", BillingCurrency.USD))
    );

    private static final Map<String, ModelPrice> BY_PROVIDER = Map.of(
            "deepseek", DEEPSEEK_DEFAULT,
            "openai", OPENAI_DEFAULT
    );

    public static Optional<ModelPrice> resolve(String providerCode, String modelName) {
        if (modelName != null) {
            String normalized = modelName.trim().toLowerCase();
            ModelPrice exact = BY_MODEL.get(normalized);
            if (exact != null) {
                return Optional.of(exact);
            }
            for (Map.Entry<String, ModelPrice> entry : BY_MODEL.entrySet()) {
                if (normalized.startsWith(entry.getKey())) {
                    return Optional.of(entry.getValue());
                }
            }
        }
        if (providerCode != null) {
            ModelPrice providerPrice = BY_PROVIDER.get(providerCode.trim().toLowerCase());
            if (providerPrice != null) {
                return Optional.of(providerPrice);
            }
        }
        return Optional.empty();
    }

    private static ModelPrice price(String inputPer1k, String outputPer1k, BillingCurrency currency) {
        return new ModelPrice(new BigDecimal(inputPer1k), new BigDecimal(outputPer1k), currency);
    }
}
