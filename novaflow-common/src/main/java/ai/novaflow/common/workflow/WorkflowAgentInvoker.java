package ai.novaflow.common.workflow;

/**
 * 工作流 Agent 节点调用端口，由 novaflow-agent 模块实现，避免 workflow ↔ agent 循环依赖。
 */
public interface WorkflowAgentInvoker {

    WorkflowAgentInvokeResult invoke(
            Long agentId,
            Long tenantId,
            Long userId,
            String input,
            String conversationKey);
}
