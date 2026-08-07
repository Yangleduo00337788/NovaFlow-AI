package ai.novaflow.agent.service;

import ai.novaflow.agent.domain.dto.AgentDebugChatRequest;
import ai.novaflow.agent.domain.vo.AgentDebugChatVO;
import ai.novaflow.agent.domain.vo.AgentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AgentDebugService {

    private final AgentService agentService;

    public AgentDebugChatVO chat(Long agentId, AgentDebugChatRequest request) {
        long start = System.currentTimeMillis();
        AgentVO agent = agentService.detail(agentId);
        String message = request.getMessage().trim();

        String reply = buildMockReply(agent, message);
        int tokens = Math.max(1, (message.length() + reply.length()) / 2);

        return AgentDebugChatVO.builder()
                .reply(reply)
                .agentName(agent.getAgentName())
                .tokensUsed(tokens)
                .latencyMs(System.currentTimeMillis() - start + 80)
                .debugMode(true)
                .build();
    }

    public AgentDebugChatVO welcome(Long agentId) {
        AgentVO agent = agentService.detail(agentId);
        String welcome = StringUtils.hasText(agent.getWelcomeMessage())
                ? agent.getWelcomeMessage()
                : "您好，我是 " + agent.getAgentName() + "，有什么可以帮您？";

        return AgentDebugChatVO.builder()
                .reply(welcome)
                .agentName(agent.getAgentName())
                .tokensUsed(welcome.length() / 2)
                .latencyMs(50L)
                .debugMode(true)
                .build();
    }

    private String buildMockReply(AgentVO agent, String message) {
        String promptHint = StringUtils.hasText(agent.getSystemPrompt())
                ? "已加载 System Prompt（" + Math.min(agent.getSystemPrompt().length(), 120) + " 字）"
                : "未配置 System Prompt";

        return switch (agent.getAgentType()) {
            case "rag" -> String.format(
                    "【RAG 调试】%s\n\n您的问题：%s\n\n（调试模式）将检索知识库并基于上下文生成回答。%s",
                    agent.getAgentName(), message, promptHint);
            case "tool" -> String.format(
                    "【Tool 调试】%s\n\n收到指令：%s\n\n（调试模式）将解析意图并调用已绑定工具。%s",
                    agent.getAgentName(), message, promptHint);
            case "workflow" -> String.format(
                    "【Workflow 调试】%s\n\n输入：%s\n\n（调试模式）将按工作流节点逐步执行。%s",
                    agent.getAgentName(), message, promptHint);
            default -> String.format(
                    "【Chat 调试】%s\n\n%s\n\n（调试模式）%s。接入模型引擎后将返回真实 AI 回复。",
                    agent.getAgentName(), message, promptHint);
        };
    }
}
