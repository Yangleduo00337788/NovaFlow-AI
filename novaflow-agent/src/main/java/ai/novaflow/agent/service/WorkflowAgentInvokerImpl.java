package ai.novaflow.agent.service;

import ai.novaflow.agent.domain.dto.AgentDebugChatRequest;
import ai.novaflow.agent.domain.vo.AgentDebugChatVO;
import ai.novaflow.agent.domain.vo.AgentVO;
import ai.novaflow.common.workflow.WorkflowAgentInvokeResult;
import ai.novaflow.common.workflow.WorkflowAgentInvoker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WorkflowAgentInvokerImpl implements WorkflowAgentInvoker {

    private final AgentService agentService;
    private final AgentChatService agentChatService;

    @Override
    public WorkflowAgentInvokeResult invoke(
            Long agentId,
            Long tenantId,
            Long userId,
            String input,
            String conversationKey) {
        if (agentId == null) {
            return WorkflowAgentInvokeResult.failure("Agent 节点未选择 Agent");
        }
        if (!StringUtils.hasText(input)) {
            return WorkflowAgentInvokeResult.failure("Agent 节点输入不能为空");
        }
        try {
            AgentVO agent = agentService.detailWithoutAccessRecord(agentId);
            if (!agentChatService.supportsRealExecution(agent)) {
                return WorkflowAgentInvokeResult.failure("所选 Agent 不支持执行，请检查配置与发布状态");
            }
            AgentDebugChatRequest request = new AgentDebugChatRequest();
            request.setMessage(input.trim());
            request.setConversationId(conversationKey);
            long resolvedUserId = userId != null && userId > 0 ? userId : 0L;
            AgentDebugChatVO result = agentChatService.chat(
                    agent,
                    request,
                    tenantId,
                    resolvedUserId,
                    "workflow-agent-node");
            String reply = result.getReply() != null ? result.getReply() : "";
            return WorkflowAgentInvokeResult.success(reply, result.getTokensUsed());
        } catch (Exception e) {
            return WorkflowAgentInvokeResult.failure("Agent 节点执行失败: " + rootMessage(e));
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : "未知错误";
    }
}
