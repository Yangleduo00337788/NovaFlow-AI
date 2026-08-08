package ai.novaflow.agent.service;

import ai.novaflow.agent.domain.dto.AgentDebugChatRequest;
import ai.novaflow.agent.domain.vo.AgentDebugChatVO;
import ai.novaflow.agent.domain.vo.AgentDebugStreamEvent;
import ai.novaflow.agent.domain.vo.AgentVO;
import ai.novaflow.agent.domain.vo.RetrievalSourceVO;
import ai.novaflow.aiengine.agent.ChatAgentExecutor;
import ai.novaflow.aiengine.agent.ChatExecuteRequest;
import ai.novaflow.aiengine.agent.ChatExecuteResult;
import ai.novaflow.common.domain.RetrievalConfig;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.model.domain.ResolvedModelConfig;
import ai.novaflow.model.domain.dto.ModelUsageRecordRequest;
import ai.novaflow.model.service.ModelResolutionService;
import ai.novaflow.model.service.ModelUsageService;
import ai.novaflow.rag.domain.RetrievedChunk;
import ai.novaflow.rag.retrieval.KnowledgeRetrievalService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentChatService {

    private final ModelResolutionService modelResolutionService;
    private final ModelUsageService modelUsageService;
    private final ChatAgentExecutor chatAgentExecutor;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    public boolean supportsRealExecution(AgentVO agent) {
        return "chat".equals(agent.getAgentType()) || "rag".equals(agent.getAgentType());
    }

    public AgentDebugChatVO chat(AgentVO agent, AgentDebugChatRequest request, Long tenantId, Long userId, String conversationPrefix) {
        String message = request.getMessage().trim();
        ChatContext context = buildChatContext(agent, request, tenantId, userId, conversationPrefix);
        ExecutionPlan plan = buildExecutionPlan(context, message);
        ChatExecuteResult result = chatAgentExecutor.execute(plan.executeRequest());
        recordUsage(context, result, "chat");
        persistConversation(context, message, result, plan.sources(), conversationPrefix);
        return toChatVO(agent, result, plan.sources());
    }

    public void streamChat(
            AgentVO agent,
            AgentDebugChatRequest request,
            Long tenantId,
            Long userId,
            String conversationPrefix,
            SseEmitter emitter) {
        String message = request.getMessage().trim();
        ChatContext context = buildChatContext(agent, request, tenantId, userId, conversationPrefix);
        ExecutionPlan plan = buildExecutionPlan(context, message);

        chatAgentExecutor.executeStream(plan.executeRequest(), new ai.novaflow.aiengine.agent.ChatStreamListener() {
            @Override
            public void onThinkingToken(String token) {
                sendEvent(emitter, AgentDebugStreamEvent.builder()
                        .type("thinking_token")
                        .content(token)
                        .build());
            }

            @Override
            public void onToken(String token) {
                sendEvent(emitter, AgentDebugStreamEvent.builder()
                        .type("token")
                        .content(token)
                        .build());
            }

            @Override
            public void onComplete(ChatExecuteResult result) {
                recordUsage(context, result, "chat");
                persistConversation(context, message, result, plan.sources(), conversationPrefix);
                sendEvent(emitter, AgentDebugStreamEvent.builder()
                        .type("done")
                        .reply(result.getReply())
                        .thinking(result.getThinking())
                        .agentName(agent.getAgentName())
                        .tokensUsed(result.getTokensUsed())
                        .latencyMs(result.getLatencyMs())
                        .debugMode(false)
                        .sources(plan.sources())
                        .build());
                emitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                completeWithError(emitter, error);
            }
        });
    }

    public void clearConversation(String conversationId) {
        if (StringUtils.hasText(conversationId)) {
            chatAgentExecutor.clearConversation(conversationId);
        }
    }

    private ExecutionPlan buildExecutionPlan(ChatContext context, String userMessage) {
        if ("rag".equals(context.agent().getAgentType())) {
            return buildRagExecutionPlan(context, userMessage);
        }
        return new ExecutionPlan(context.toExecuteRequest(userMessage, context.agent().getSystemPrompt()), List.of());
    }

    private ExecutionPlan buildRagExecutionPlan(ChatContext context, String userMessage) {
        List<Long> knowledgeBaseIds = context.agent().getKnowledgeBaseIds();
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            throw new BusinessException("RAG Agent 未关联知识库，请先在配置中绑定");
        }

        List<RetrievedChunk> chunks = knowledgeRetrievalService.retrieveAcrossKnowledgeBases(
                knowledgeBaseIds,
                context.tenantId(),
                userMessage,
                resolveRetrievalTopK(context.agent()),
                resolveRetrievalScoreThreshold(context.agent()));
        String contextBlock = knowledgeRetrievalService.buildContextPrompt(chunks);
        String systemPrompt = buildRagSystemPrompt(context.agent().getSystemPrompt(), contextBlock);
        List<RetrievalSourceVO> sources = chunks.stream().map(this::toSourceVO).toList();
        return new ExecutionPlan(context.toExecuteRequest(userMessage, systemPrompt), sources);
    }

    private String buildRagSystemPrompt(String userPrompt, String contextBlock) {
        String basePrompt = StringUtils.hasText(userPrompt)
                ? userPrompt
                : "你是一个基于企业知识库回答问题的助手。";
        if (!StringUtils.hasText(contextBlock)) {
            return basePrompt + "\n\n当前未检索到相关参考资料，请如实告知用户。";
        }
        return basePrompt + "\n\n请根据以下参考资料回答用户问题。若参考资料中没有相关信息，请如实说明。\n\n参考资料：\n"
                + contextBlock;
    }

    private int resolveRetrievalTopK(AgentVO agent) {
        if (agent.getRetrievalTopK() != null && agent.getRetrievalTopK() > 0) {
            return agent.getRetrievalTopK();
        }
        return RetrievalConfig.DEFAULT_TOP_K;
    }

    private Float resolveRetrievalScoreThreshold(AgentVO agent) {
        return agent.getRetrievalScoreThreshold();
    }

    private void persistConversation(
            ChatContext context,
            String userMessage,
            ChatExecuteResult result,
            List<RetrievalSourceVO> sources,
            String channel) {
        try {
            conversationService.persistExchange(ConversationService.ExchangeRequest.builder()
                    .tenantId(context.tenantId())
                    .agentId(context.agent().getId())
                    .conversationKey(context.conversationId())
                    .channel(channel)
                    .userId(context.userId())
                    .userMessage(userMessage)
                    .assistantReply(result.getReply())
                    .tokensUsed(result.getTokensUsed())
                    .latencyMs(result.getLatencyMs())
                    .sources(sources)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to persist conversation, agentId={}, key={}",
                    context.agent().getId(), context.conversationId(), e);
        }
    }

    private RetrievalSourceVO toSourceVO(RetrievedChunk chunk) {
        return RetrievalSourceVO.builder()
                .knowledgeBaseId(chunk.getKnowledgeBaseId())
                .knowledgeBaseName(chunk.getKnowledgeBaseName())
                .documentId(chunk.getDocumentId())
                .docName(chunk.getDocName())
                .chunkIndex(chunk.getChunkIndex())
                .text(chunk.getText())
                .score(chunk.getScore())
                .build();
    }

    private ChatContext buildChatContext(
            AgentVO agent,
            AgentDebugChatRequest request,
            Long tenantId,
            Long userId,
            String conversationPrefix) {
        ResolvedModelConfig modelConfig = modelResolutionService.resolve(
                agent.getModelConfigId(),
                tenantId,
                agent.getTemperature(),
                agent.getMaxTokens()
        );
        modelConfig.setEnableDeepThinking(Boolean.TRUE.equals(request.getEnableDeepThinking()));
        modelConfig.setEnableWebSearch(Boolean.TRUE.equals(request.getEnableWebSearch()));
        String prefix = StringUtils.hasText(conversationPrefix) ? conversationPrefix : "chat";
        String conversationId = StringUtils.hasText(request.getConversationId())
                ? request.getConversationId()
                : prefix + "-" + agent.getId();
        return new ChatContext(agent, tenantId, userId, modelConfig, conversationId, agent.getMemoryWindow());
    }

    private void recordUsage(ChatContext context, ChatExecuteResult result, String usageType) {
        modelUsageService.record(ModelUsageRecordRequest.builder()
                .tenantId(context.tenantId())
                .applicationId(context.agent().getApplicationId())
                .agentId(context.agent().getId())
                .userId(context.userId())
                .modelConfigId(context.modelConfig().getModelConfigId())
                .usageType(usageType)
                .inputTokens(result.getInputTokens())
                .outputTokens(result.getOutputTokens())
                .totalTokens(result.getTokensUsed())
                .latencyMs(result.getLatencyMs())
                .build());
    }

    private AgentDebugChatVO toChatVO(AgentVO agent, ChatExecuteResult result, List<RetrievalSourceVO> sources) {
        return AgentDebugChatVO.builder()
                .reply(result.getReply())
                .agentName(agent.getAgentName())
                .tokensUsed(result.getTokensUsed())
                .latencyMs(result.getLatencyMs())
                .debugMode(false)
                .sources(sources)
                .build();
    }

    private void sendEvent(SseEmitter emitter, AgentDebugStreamEvent event) {
        try {
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException e) {
            throw new BusinessException("流式响应序列化失败");
        } catch (IOException e) {
            throw new BusinessException("流式响应发送失败");
        }
    }

    private void completeWithError(SseEmitter emitter, Throwable error) {
        try {
            String message = error instanceof BusinessException businessException
                    ? businessException.getMessage()
                    : rootMessage(error);
            sendEvent(emitter, AgentDebugStreamEvent.builder()
                    .type("error")
                    .message(message)
                    .build());
            emitter.complete();
        } catch (Exception ex) {
            emitter.completeWithError(error);
        }
    }

    private String rootMessage(Throwable e) {
        Throwable current = e;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : e.getMessage();
    }

    private record ExecutionPlan(ChatExecuteRequest executeRequest, List<RetrievalSourceVO> sources) {
    }

    private record ChatContext(
            AgentVO agent,
            Long tenantId,
            Long userId,
            ResolvedModelConfig modelConfig,
            String conversationId,
            Integer memoryWindow
    ) {
        ChatExecuteRequest toExecuteRequest(String userMessage, String systemPrompt) {
            return ChatExecuteRequest.builder()
                    .modelConfig(modelConfig)
                    .systemPrompt(systemPrompt)
                    .userMessage(userMessage)
                    .conversationId(conversationId)
                    .memoryWindow(memoryWindow)
                    .build();
        }
    }
}
