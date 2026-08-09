package ai.novaflow.agent.service;

import ai.novaflow.agent.domain.dto.AgentDebugChatRequest;
import ai.novaflow.agent.domain.vo.AgentDebugChatVO;
import ai.novaflow.agent.domain.vo.AgentDebugStreamEvent;
import ai.novaflow.agent.domain.vo.AgentVO;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.workflow.domain.WorkflowExecutionStatus;
import ai.novaflow.workflow.domain.dto.WorkflowRunOptions;
import ai.novaflow.workflow.domain.dto.WorkflowRunRequest;
import ai.novaflow.workflow.domain.vo.WorkflowRunResultVO;
import ai.novaflow.workflow.service.WorkflowExecutionService;
import ai.novaflow.workflow.service.WorkflowService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentWorkflowChatService {

    private final WorkflowExecutionService workflowExecutionService;
    private final WorkflowService workflowService;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    public AgentDebugChatVO chat(
            AgentVO agent,
            AgentDebugChatRequest request,
            Long tenantId,
            Long userId,
            String conversationPrefix) {
        WorkflowRunResultVO result = runWorkflow(agent, request.getMessage().trim(), tenantId, userId);
        String reply = resolveReply(result);
        long latencyMs = toLatencyMs(result.getDurationMs());
        int tokensUsed = result.getTokensUsed() != null ? result.getTokensUsed() : 0;
        AgentDebugChatVO vo = AgentDebugChatVO.builder()
                .reply(reply)
                .agentName(agent.getAgentName())
                .tokensUsed(tokensUsed)
                .latencyMs(latencyMs)
                .debugMode(false)
                .build();
        persistConversation(agent, request, tenantId, userId, conversationPrefix, reply, latencyMs, tokensUsed);
        return vo;
    }

    public void streamChat(
            AgentVO agent,
            AgentDebugChatRequest request,
            Long tenantId,
            Long userId,
            String conversationPrefix,
            SseEmitter emitter) {
        try {
            WorkflowRunResultVO result = runWorkflow(agent, request.getMessage().trim(), tenantId, userId);
            String reply = resolveReply(result);
            long latencyMs = toLatencyMs(result.getDurationMs());
            int tokensUsed = result.getTokensUsed() != null ? result.getTokensUsed() : 0;
            for (char ch : reply.toCharArray()) {
                sendEvent(emitter, AgentDebugStreamEvent.builder()
                        .type("token")
                        .content(String.valueOf(ch))
                        .build());
            }
            persistConversation(agent, request, tenantId, userId, conversationPrefix, reply, latencyMs, tokensUsed);
            sendEvent(emitter, AgentDebugStreamEvent.builder()
                    .type("done")
                    .reply(reply)
                    .agentName(agent.getAgentName())
                    .tokensUsed(tokensUsed)
                    .latencyMs(latencyMs)
                    .debugMode(false)
                    .build());
            emitter.complete();
        } catch (BusinessException e) {
            completeWithError(emitter, e);
        } catch (Exception e) {
            completeWithError(emitter, new BusinessException("工作流执行失败: " + e.getMessage()));
        }
    }

    private WorkflowRunResultVO runWorkflow(AgentVO agent, String message, Long tenantId, Long userId) {
        if (agent.getWorkflowId() == null) {
            throw new BusinessException("Workflow Agent 未绑定工作流");
        }
        workflowService.requirePublishedWorkflow(agent.getWorkflowId(), tenantId);
        WorkflowRunRequest runRequest = new WorkflowRunRequest();
        runRequest.setInput(message);
        WorkflowRunResultVO result = workflowExecutionService.run(
                agent.getWorkflowId(),
                runRequest,
                WorkflowRunOptions.builder()
                        .triggeredByUserId(userId)
                        .agentId(agent.getId())
                        .recordUsage(true)
                        .build());
        if (!Integer.valueOf(WorkflowExecutionStatus.SUCCESS).equals(result.getStatus())) {
            throw new BusinessException(StringUtils.hasText(result.getErrorMessage())
                    ? result.getErrorMessage()
                    : "工作流执行失败");
        }
        return result;
    }

    private String resolveReply(WorkflowRunResultVO result) {
        if (StringUtils.hasText(result.getOutput())) {
            return result.getOutput();
        }
        return "工作流执行完成，但未返回输出";
    }

    private long toLatencyMs(Integer durationMs) {
        return durationMs == null ? 0L : durationMs.longValue();
    }

    private void persistConversation(
            AgentVO agent,
            AgentDebugChatRequest request,
            Long tenantId,
            Long userId,
            String conversationPrefix,
            String reply,
            long latencyMs,
            int tokensUsed) {
        try {
            String prefix = StringUtils.hasText(conversationPrefix) ? conversationPrefix : "workflow";
            String conversationKey = StringUtils.hasText(request.getConversationId())
                    ? request.getConversationId()
                    : prefix + "-" + agent.getId();
            conversationService.persistExchange(ConversationService.ExchangeRequest.builder()
                    .tenantId(tenantId)
                    .agentId(agent.getId())
                    .conversationKey(conversationKey)
                    .channel(prefix)
                    .userId(userId)
                    .userMessage(request.getMessage().trim())
                    .assistantReply(reply)
                    .tokensUsed(tokensUsed)
                    .latencyMs(latencyMs)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to persist workflow conversation, agentId={}", agent.getId(), e);
        }
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

    private void completeWithError(SseEmitter emitter, BusinessException error) {
        try {
            sendEvent(emitter, AgentDebugStreamEvent.builder()
                    .type("error")
                    .message(error.getMessage())
                    .build());
            emitter.complete();
        } catch (Exception ex) {
            emitter.completeWithError(error);
        }
    }
}
