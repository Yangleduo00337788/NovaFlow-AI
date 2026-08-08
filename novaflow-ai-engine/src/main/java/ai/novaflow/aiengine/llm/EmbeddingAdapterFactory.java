package ai.novaflow.aiengine.llm;

import ai.novaflow.model.domain.ResolvedModelConfig;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class EmbeddingAdapterFactory {

    public EmbeddingModel createEmbeddingModel(ResolvedModelConfig config) {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .timeout(Duration.ofSeconds(120))
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}
