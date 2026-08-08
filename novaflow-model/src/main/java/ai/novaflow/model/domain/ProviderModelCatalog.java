package ai.novaflow.model.domain;

import ai.novaflow.model.domain.UpstreamModelDescriptor;

import java.util.List;
import java.util.Map;

public final class ProviderModelCatalog {

    private ProviderModelCatalog() {
    }

    private static final Map<String, List<UpstreamModelDescriptor>> BY_PROVIDER = Map.ofEntries(
            Map.entry("claude", List.of(
                    chat("claude-sonnet-4-20250514", "Claude Sonnet 4"),
                    chat("claude-3-5-sonnet-latest", "Claude 3.5 Sonnet"),
                    chat("claude-3-5-haiku-latest", "Claude 3.5 Haiku"),
                    chat("claude-3-opus-latest", "Claude 3 Opus")
            )),
            Map.entry("doubao", List.of(
                    chat("doubao-pro-32k", "豆包 Pro 32K"),
                    chat("doubao-pro-128k", "豆包 Pro 128K"),
                    chat("doubao-lite-32k", "豆包 Lite 32K")
            )),
            Map.entry("baidu", List.of(
                    chat("ernie-4.0-8k", "ERNIE 4.0 8K"),
                    chat("ernie-3.5-8k", "ERNIE 3.5 8K"),
                    chat("ernie-speed-128k", "ERNIE Speed 128K")
            )),
            Map.entry("gemini", List.of(
                    chat("gemini-2.0-flash", "Gemini 2.0 Flash"),
                    chat("gemini-1.5-pro", "Gemini 1.5 Pro"),
                    chat("gemini-1.5-flash", "Gemini 1.5 Flash")
            ))
    );

    public static List<UpstreamModelDescriptor> list(String providerCode) {
        if (providerCode == null) {
            return List.of();
        }
        return BY_PROVIDER.getOrDefault(providerCode.toLowerCase(), List.of());
    }

    private static UpstreamModelDescriptor chat(String modelName, String displayName) {
        return UpstreamModelDescriptor.builder()
                .modelName(modelName)
                .modelType("chat")
                .displayName(displayName)
                .build();
    }
}
