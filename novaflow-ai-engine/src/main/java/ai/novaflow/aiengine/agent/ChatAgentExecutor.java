package ai.novaflow.aiengine.agent;

import ai.novaflow.aiengine.llm.LlmAdapterFactory;
import ai.novaflow.aiengine.llm.OpenAiCompatibleChatClient;
import ai.novaflow.aiengine.llm.OpenAiCompatibleStreamClient;
import ai.novaflow.model.domain.ModelExtraParametersBuilder;
import ai.novaflow.model.domain.ResolvedModelConfig;
import ai.novaflow.tool.domain.HttpToolDefinition;
import ai.novaflow.tool.executor.ToolExecutorRouter;
import ai.novaflow.tool.schema.ToolSchemaBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    private final OpenAiCompatibleChatClient openAiCompatibleChatClient;
    private final ToolExecutorRouter toolExecutorRouter;
    private final ToolSchemaBuilder toolSchemaBuilder;
    private final ObjectMapper objectMapper;

    private static final int MAX_TOOL_ROUNDS = 5;

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

    public void executeToolStream(ChatExecuteRequest request, ChatStreamListener listener) {
        List<HttpToolDefinition> tools = request.getTools() != null ? request.getTools() : List.of();
        if (tools.isEmpty()) {
            executeStream(request, listener);
            return;
        }

        long start = System.currentTimeMillis();
        MessageWindowChatMemory memory = buildMemory(request);
        ArrayNode messages = buildOpenAiMessageNodes(request, memory);
        ArrayNode toolSpecs = toolSchemaBuilder.toOpenAiTools(tools);
        OpenAiCompatibleStreamClient.TokenUsageSummary usageSummary = new OpenAiCompatibleStreamClient.TokenUsageSummary();

        try {
            for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
                OpenAiCompatibleChatClient.ChatCompletionResponse response =
                        openAiCompatibleChatClient.chat(request.getModelConfig(), messages, toolSpecs);
                if (response.getUsage() != null) {
                    usageSummary.updateFrom(response.getUsage());
                }

                if (response.getToolCalls() == null || response.getToolCalls().isEmpty()) {
                    String reply = response.getContent() != null ? response.getContent() : "";
                    streamTextTokens(reply, listener);
                    memory.add(UserMessage.from(request.getUserMessage()));
                    memory.add(AiMessage.from(reply));
                    listener.onComplete(ChatExecuteResult.builder()
                            .reply(reply)
                            .tokensUsed(usageSummary.totalTokens())
                            .inputTokens(usageSummary.inputTokens())
                            .outputTokens(usageSummary.outputTokens())
                            .latencyMs(System.currentTimeMillis() - start)
                            .modelName(request.getModelConfig().getModelName())
                            .build());
                    return;
                }

                ObjectNode assistantMessage = messages.addObject();
                assistantMessage.put("role", "assistant");
                assistantMessage.put("content", response.getContent() != null ? response.getContent() : "");
                ArrayNode toolCallsNode = assistantMessage.putArray("tool_calls");
                for (OpenAiCompatibleChatClient.ToolCallItem toolCall : response.getToolCalls()) {
                    ObjectNode toolCallNode = toolCallsNode.addObject();
                    toolCallNode.put("id", toolCall.getId());
                    toolCallNode.put("type", "function");
                    ObjectNode function = toolCallNode.putObject("function");
                    function.put("name", toolCall.getName());
                    function.put("arguments", toolCall.getArguments());

                    HttpToolDefinition definition = findTool(tools, toolCall.getName());
                    listener.onToolCall(toolCall.getName(), toolCall.getArguments());
                    String result = toolExecutorRouter.execute(
                            definition,
                            toolSchemaBuilder.parseArguments(toolCall.getArguments()));
                    listener.onToolResult(toolCall.getName(), result);

                    ObjectNode toolMessage = messages.addObject();
                    toolMessage.put("role", "tool");
                    toolMessage.put("tool_call_id", toolCall.getId());
                    toolMessage.put("content", result);
                }
            }
            listener.onError(new IllegalStateException("工具调用轮次超过上限"));
        } catch (Exception error) {
            listener.onError(error);
        }
    }

    private void streamTextTokens(String text, ChatStreamListener listener) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int chunkSize = text.length() > 48 ? 2 : 1;
        for (int i = 0; i < text.length(); i += chunkSize) {
            listener.onToken(text.substring(i, Math.min(text.length(), i + chunkSize)));
        }
    }

    private HttpToolDefinition findTool(List<HttpToolDefinition> tools, String name) {
        return tools.stream()
                .filter(tool -> tool.getName() != null && tool.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到工具: " + name));
    }

    private ArrayNode buildOpenAiMessageNodes(ChatExecuteRequest request, MessageWindowChatMemory memory) {
        ArrayNode messages = objectMapper.createArrayNode();
        if (StringUtils.hasText(request.getSystemPrompt())) {
            ObjectNode system = messages.addObject();
            system.put("role", "system");
            system.put("content", request.getSystemPrompt());
        }
        for (ChatMessage message : memory.messages()) {
            if (message instanceof SystemMessage systemMessage) {
                ObjectNode node = messages.addObject();
                node.put("role", "system");
                node.put("content", systemMessage.text());
            } else if (message instanceof UserMessage userMessage) {
                ObjectNode node = messages.addObject();
                node.put("role", "user");
                node.put("content", userMessage.singleText());
            } else if (message instanceof AiMessage aiMessage) {
                ObjectNode node = messages.addObject();
                node.put("role", "assistant");
                node.put("content", aiMessage.text());
            }
        }
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", request.getUserMessage());
        return messages;
    }

    private void executeStreamWithExtraParameters(ChatExecuteRequest request, ChatStreamListener listener) {
        MessageWindowChatMemory memory = buildMemory(request);
        List<Map<String, String>> messages = buildOpenAiMessages(request, memory);
        long start = System.currentTimeMillis();
        StringBuilder replyBuilder = new StringBuilder();
        StringBuilder thinkingBuilder = new StringBuilder();
        final OpenAiCompatibleStreamClient.TokenUsageSummary usageSummary =
                new OpenAiCompatibleStreamClient.TokenUsageSummary();
        final List<WebSearchSource> webSearchSources = new ArrayList<>();
        final boolean forwardThinking = Boolean.TRUE.equals(request.getModelConfig().getEnableDeepThinking());

        try {
            openAiCompatibleStreamClient.streamChat(
                    request.getModelConfig(),
                    messages,
                    token -> {
                        if (forwardThinking) {
                            thinkingBuilder.append(token);
                            listener.onThinkingToken(token);
                        }
                    },
                    token -> {
                        replyBuilder.append(token);
                        listener.onToken(token);
                    },
                    usage -> usageSummary.updateFrom(usage),
                    sources -> {
                        webSearchSources.clear();
                        webSearchSources.addAll(sources);
                        listener.onWebSearchSources(sources);
                    });

            String reply = replyBuilder.toString();
            String thinking = forwardThinking ? thinkingBuilder.toString() : "";
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
                    .webSearchSources(webSearchSources.isEmpty() ? null : new ArrayList<>(webSearchSources))
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
