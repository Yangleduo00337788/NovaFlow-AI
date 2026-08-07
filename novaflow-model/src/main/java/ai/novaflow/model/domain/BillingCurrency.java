package ai.novaflow.model.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BillingCurrency {

    CNY("CNY", "¥"),
    USD("USD", "$");

    private final String code;
    private final String symbol;

    public static BillingCurrency fromCode(String code) {
        if (code == null) {
            return CNY;
        }
        for (BillingCurrency currency : values()) {
            if (currency.code.equalsIgnoreCase(code)) {
                return currency;
            }
        }
        return CNY;
    }

    public static BillingCurrency fromProviderCode(String providerCode) {
        return ModelProviderPreset.of(providerCode)
                .map(ModelProviderPreset::getCurrency)
                .orElse(USD);
    }
}
