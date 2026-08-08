package ai.novaflow.aiengine.agent;

import ai.novaflow.aiengine.llm.LlmAdapterFactory;
import ai.novaflow.aiengine.llm.OpenAiCompatibleStreamClient;
import ai.novaflow.model.domain.ModelExtraParametersBuilder;
import ai.novaflow.model.domain.ResolvedModelConfig;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatAgentExecutor {

    private final LlmAdapterFactory llmAdapterFactory;
    private final ChatMemoryStore chatMemoryStore;
    private final OpenAiCompatibleStreamClient openAiCompatibleStreamClient;

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
        if (hasExtraParameters(request.getModelConfig())) {
            executeStreamWithExtraParameters(request, listener);
            return;
        }

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

    private void executeStreamWithExtraParameters(ChatExecuteRequest request, ChatStreamListener listener) {
        MessageWindowChatMemory memory = buildMemory(request);
        List<Map<String, String>> messages = buildOpenAiMessages(request, memory);
        long start = System.currentTimeMillis();
        StringBuilder replyBuilder = new StringBuilder();
        StringBuilder thinkingBuilder = new StringBuilder();
        final OpenAiCompatibleStreamClient.TokenUsageSummary usageSummary =
                new OpenAiCompatibleStreamClient.TokenUsageSummary();

        try {
            openAiCompatibleStreamClient.streamChat(
                    request.getModelConfig(),
                    messages,
                    token -> {
                        thinkingBuilder.append(token);
                        listener.onThinkingToken(token);
                    },
                    token -> {
                        replyBuilder.append(token);
                        listener.onToken(token);
                    },
                    usage -> usageSummary.updateFrom(usage));

            String reply = replyBuilder.toString();
            String thinking = thinkingBuilder.toString();
            memory.add(UserMessage.from(request.getUserMessage()));
            memory.add(AiMessage.from(reply));

            ChatExecuteResult result = ChatExecuteResult.builder()
                    .reply(reply)
                    .thinking(thinking)
                    .tokensUsed(usageSummary.totalTokens())
                    .inputTokens(usageSummary.inputTokens())
                    .outputTokens(usageSummary.outputTokens())
                    .latencyMs(System.currentTimeMillis() - start)
                    .modelName(request.getModelConfig().getModelName())
                    .build();
            listener.onComplete(result);
        } catch (Exception error) {
            listener.onError(error);
        }
    }

    private boolean hasExtraParameters(ResolvedModelConfig config) {
        return !ModelExtraParametersBuilder.build(config).isEmpty();
    }

    private List<Map<String, String>> buildOpenAiMessages(ChatExecuteRequest request, MessageWindowChatMemory memory) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (StringUtils.hasText(request.getSystemPrompt())) {
            messages.add(Map.of("role", "system", "content", request.getSystemPrompt()));
        }
        for (ChatMessage message : memory.messages()) {
            if (message instanceof SystemMessage systemMessage) {
                messages.add(Map.of("role", "system", "content", systemMessage.text()));
            } else if (message instanceof UserMessage userMessage) {
                messages.add(Map.of("role", "user", "content", userMessage.singleText()));
            } else if (message instanceof AiMessage aiMessage) {
                messages.add(Map.of("role", "assistant", "content", aiMessage.text()));
            }
        }
        messages.add(Map.of("role", "user", "content", request.getUserMessage()));
        return messages;
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
