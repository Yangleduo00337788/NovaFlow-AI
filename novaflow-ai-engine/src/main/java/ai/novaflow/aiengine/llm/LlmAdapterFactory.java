package ai.novaflow.aiengine.llm;

import ai.novaflow.model.domain.ResolvedModelConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class LlmAdapterFactory {

    public ChatLanguageModel createChatModel(ResolvedModelConfig config) {
        return OpenAiChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .temperature(config.getTemperature().doubleValue())
                .maxTokens(config.getMaxTokens())
                .timeout(Duration.ofSeconds(120))
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}
