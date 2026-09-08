package ai.novaflow.aiengine.agent;

import ai.novaflow.aiengine.llm.LlmAdapterFactory;
import ai.novaflow.aiengine.llm.OpenAiCompatibleChatClient;
import ai.novaflow.aiengine.llm.OpenAiCompatibleStreamClient;
import ai.novaflow.model.domain.ResolvedModelConfig;
import ai.novaflow.tool.domain.HttpToolDefinition;
import ai.novaflow.tool.executor.ToolExecutorRouter;
import ai.novaflow.tool.schema.ToolSchemaBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatAgentExecutorTest {

    @Mock
    private LlmAdapterFactory llmAdapterFactory;
    @Mock
    private ChatMemoryStore chatMemoryStore;
    @Mock
    private OpenAiCompatibleStreamClient openAiCompatibleStreamClient;
    @Mock
    private OpenAiCompatibleChatClient openAiCompatibleChatClient;
    @Mock
    private ToolExecutorRouter toolExecutorRouter;
    @Mock
    private ToolSchemaBuilder toolSchemaBuilder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ChatAgentExecutor chatAgentExecutor;

    @org.junit.jupiter.api.BeforeEach
    void setUpExecutor() {
        chatAgentExecutor = new ChatAgentExecutor(
                llmAdapterFactory,
                chatMemoryStore,
                openAiCompatibleStreamClient,
                openAiCompatibleChatClient,
                toolExecutorRouter,
                toolSchemaBuilder,
                objectMapper
        );
    }

    @Test
    void clearConversationIgnoresBlankId() {
        chatAgentExecutor.clearConversation(null);
        chatAgentExecutor.clearConversation("  ");
        verify(chatMemoryStore, never()).deleteMessages(anyString());
    }

    @Test
    void clearConversationDeletesMessages() {
        chatAgentExecutor.clearConversation("conv-1");
        verify(chatMemoryStore).deleteMessages("conv-1");
    }

    @Test
    void executeToolStreamRejectsUnknownTool() throws Exception {
        OpenAiCompatibleChatClient.ToolCallItem toolCall = new OpenAiCompatibleChatClient.ToolCallItem();
        toolCall.setId("tc-1");
        toolCall.setName("missing-tool");
        toolCall.setArguments("{}");

        OpenAiCompatibleChatClient.ChatCompletionResponse response = new OpenAiCompatibleChatClient.ChatCompletionResponse();
        response.setToolCalls(List.of(toolCall));

        when(toolSchemaBuilder.toOpenAiTools(any())).thenReturn(objectMapper.createArrayNode());
        when(openAiCompatibleChatClient.chat(any(), any(ArrayNode.class), any(ArrayNode.class))).thenReturn(response);

        HttpToolDefinition tool = new HttpToolDefinition();
        tool.setName("known-tool");

        ChatExecuteRequest request = ChatExecuteRequest.builder()
                .modelConfig(ResolvedModelConfig.builder().modelName("gpt-test").baseUrl("http://localhost").build())
                .userMessage("hello")
                .conversationId("conv-tool")
                .tools(List.of(tool))
                .build();

        CapturingListener listener = new CapturingListener();
        chatAgentExecutor.executeToolStream(request, listener);

        assertTrue(listener.error instanceof IllegalStateException);
        assertTrue(listener.error.getMessage().contains("未找到工具"));
    }

    @Test
    void executeToolStreamStopsAfterMaxRounds() throws Exception {
        OpenAiCompatibleChatClient.ToolCallItem toolCall = new OpenAiCompatibleChatClient.ToolCallItem();
        toolCall.setId("tc-1");
        toolCall.setName("lookup");
        toolCall.setArguments("{}");

        OpenAiCompatibleChatClient.ChatCompletionResponse response = new OpenAiCompatibleChatClient.ChatCompletionResponse();
        response.setToolCalls(List.of(toolCall));

        HttpToolDefinition tool = new HttpToolDefinition();
        tool.setName("lookup");

        when(toolSchemaBuilder.toOpenAiTools(any())).thenReturn(objectMapper.createArrayNode());
        when(toolSchemaBuilder.parseArguments(anyString())).thenReturn(java.util.Map.of());
        when(toolExecutorRouter.execute(any(), any())).thenReturn("ok");
        when(openAiCompatibleChatClient.chat(any(), any(ArrayNode.class), any(ArrayNode.class))).thenReturn(response);

        ChatExecuteRequest request = ChatExecuteRequest.builder()
                .modelConfig(ResolvedModelConfig.builder().modelName("gpt-test").baseUrl("http://localhost").build())
                .userMessage("hello")
                .conversationId("conv-max")
                .tools(List.of(tool))
                .build();

        CapturingListener listener = new CapturingListener();
        chatAgentExecutor.executeToolStream(request, listener);

        assertTrue(listener.error instanceof IllegalStateException);
        assertTrue(listener.error.getMessage().contains("工具调用轮次超过上限"));
    }

    private static final class CapturingListener implements ChatStreamListener {
        private Throwable error;

        @Override
        public void onToken(String token) {
            // no-op
        }

        @Override
        public void onError(Throwable error) {
            this.error = error;
        }
    }
}
