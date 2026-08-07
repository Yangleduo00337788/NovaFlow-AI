package ai.novaflow.aiengine.agent;

import ai.novaflow.aiengine.llm.LlmAdapterFactory;
import ai.novaflow.aiengine.memory.InMemoryChatMemoryStore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatAgentExecutor {

    private final LlmAdapterFactory llmAdapterFactory;
    private final InMemoryChatMemoryStore chatMemoryStore;

    public ChatExecuteResult execute(ChatExecuteRequest request) {
        ChatLanguageModel chatModel = llmAdapterFactory.createChatModel(request.getModelConfig());
        String memoryId = StringUtils.hasText(request.getConversationId())
                ? request.getConversationId()
                : "default";

        int memoryWindow = request.getMemoryWindow() != null && request.getMemoryWindow() > 0
                ? request.getMemoryWindow()
                : 10;

        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(memoryWindow)
                .chatMemoryStore(chatMemoryStore)
                .build();

        List<ChatMessage> messages = new ArrayList<>();
        if (StringUtils.hasText(request.getSystemPrompt())) {
            messages.add(SystemMessage.from(request.getSystemPrompt()));
        }
        messages.addAll(memory.messages());
        messages.add(UserMessage.from(request.getUserMessage()));

        long start = System.currentTimeMillis();
        Response<AiMessage> response = chatModel.generate(messages);
        String reply = response.content().text();

        memory.add(UserMessage.from(request.getUserMessage()));
        memory.add(response.content());

        int inputTokens = 0;
        int outputTokens = 0;
        int tokensUsed = 0;
        TokenUsage tokenUsage = response.tokenUsage();
        if (tokenUsage != null) {
            if (tokenUsage.inputTokenCount() != null) {
                inputTokens = tokenUsage.inputTokenCount();
            }
            if (tokenUsage.outputTokenCount() != null) {
                outputTokens = tokenUsage.outputTokenCount();
            }
            if (tokenUsage.totalTokenCount() != null) {
                tokensUsed = tokenUsage.totalTokenCount();
            }
        }
        if (tokensUsed == 0) {
            tokensUsed = inputTokens + outputTokens;
        }

        return ChatExecuteResult.builder()
                .reply(reply)
                .tokensUsed(tokensUsed)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .latencyMs(System.currentTimeMillis() - start)
                .modelName(request.getModelConfig().getModelName())
                .build();
    }

    public void clearConversation(String conversationId) {
        if (StringUtils.hasText(conversationId)) {
            chatMemoryStore.deleteMessages(conversationId);
        }
    }
}
