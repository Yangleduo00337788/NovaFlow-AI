package ai.novaflow.model.domain;

import java.util.List;
import java.util.Map;

public final class EmbeddingModelCatalog {

    public record EmbeddingPreset(String modelName, String displayName) {
    }

    private static final Map<String, List<EmbeddingPreset>> BY_PROVIDER = Map.of(
            "openai", List.of(
                    new EmbeddingPreset("text-embedding-3-small", "Text Embedding 3 Small"),
                    new EmbeddingPreset("text-embedding-3-large", "Text Embedding 3 Large"),
                    new EmbeddingPreset("text-embedding-ada-002", "Text Embedding Ada 002")
            ),
            "qwen", List.of(
                    new EmbeddingPreset("text-embedding-v3", "通义 Embedding V3"),
                    new EmbeddingPreset("text-embedding-v2", "通义 Embedding V2")
            ),
            "zhipu", List.of(
                    new EmbeddingPreset("embedding-3", "智谱 Embedding-3"),
                    new EmbeddingPreset("embedding-2", "智谱 Embedding-2")
            ),
            "siliconflow", List.of(
                    new EmbeddingPreset("BAAI/bge-m3", "BGE-M3"),
                    new EmbeddingPreset("BAAI/bge-large-zh-v1.5", "BGE Large ZH v1.5")
            )
    );

    private EmbeddingModelCatalog() {
    }

    public static List<EmbeddingPreset> forProvider(String providerCode) {
        if (providerCode == null) {
            return List.of();
        }
        return BY_PROVIDER.getOrDefault(providerCode.toLowerCase(), List.of());
    }
}
