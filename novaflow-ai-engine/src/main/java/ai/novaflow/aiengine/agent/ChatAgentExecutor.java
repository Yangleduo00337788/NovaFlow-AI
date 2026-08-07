package ai.novaflow.aiengine.agent;

import ai.novaflow.aiengine.llm.LlmAdapterFactory;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatAgentExecutor {

    private final LlmAdapterFactory llmAdapterFactory;
    private final ChatMemoryStore chatMemoryStore;

    public ChatExecuteResult execute(ChatExecuteRequest request) {
        ChatLanguageModel chatModel = llmAdapterFactory.createChatModel(request.getModelConfig());
        MessageWindowChatMemory memory = buildMemory(request);

        List<ChatMessage> messages = buildMessages(request, memory);

        long start = System.currentTimeMillis();
        Response<AiMessage> response = chatModel.generate(messages);
        String reply = response.content().text();

        memory.add(UserMessage.from(request.getUserMessage()));
        memory.add(response.content());

        return buildResult(request, response, reply, System.currentTimeMillis() - start);
    }

    public void executeStream(ChatExecuteRequest request, ChatStreamListener listener) {
        StreamingChatLanguageModel chatModel = llmAdapterFactory.createStreamingChatModel(request.getModelConfig());
        MessageWindowChatMemory memory = buildMemory(request);
        List<ChatMessage> messages = buildMessages(request, memory);

        long start = System.currentTimeMillis();
        StringBuilder replyBuilder = new StringBuilder();

        chatModel.generate(messages, new StreamingResponseHandler<>() {
            @Override
            public void onNext(String token) {
                replyBuilder.append(token);
                listener.onToken(token);
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                String reply = replyBuilder.length() > 0 ? replyBuilder.toString() : response.content().text();
                memory.add(UserMessage.from(request.getUserMessage()));
                memory.add(AiMessage.from(reply));
                listener.onComplete(buildResult(request, response, reply, System.currentTimeMillis() - start));
            }

            @Override
            public void onError(Throwable error) {
                listener.onError(error);
            }
        });
    }

    public void clearConversation(String conversationId) {
        if (StringUtils.hasText(conversationId)) {
            chatMemoryStore.deleteMessages(conversationId);
        }
    }

    private MessageWindowChatMemory buildMemory(ChatExecuteRequest request) {
        String memoryId = StringUtils.hasText(request.getConversationId())
                ? request.getConversationId()
                : "default";

        int memoryWindow = request.getMemoryWindow() != null && request.getMemoryWindow() > 0
                ? request.getMemoryWindow()
                : 10;

        return MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(memoryWindow)
                .chatMemoryStore(chatMemoryStore)
                .build();
    }

    private List<ChatMessage> buildMessages(ChatExecuteRequest request, MessageWindowChatMemory memory) {
        List<ChatMessage> messages = new ArrayList<>();
        if (StringUtils.hasText(request.getSystemPrompt())) {
            messages.add(SystemMessage.from(request.getSystemPrompt()));
        }
        messages.addAll(memory.messages());
        messages.add(UserMessage.from(request.getUserMessage()));
        return messages;
    }

    private ChatExecuteResult buildResult(ChatExecuteRequest request, Response<AiMessage> response,
                                          String reply, long latencyMs) {
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
                .latencyMs(latencyMs)
                .modelName(request.getModelConfig().getModelName())
                .build();
    }
}
