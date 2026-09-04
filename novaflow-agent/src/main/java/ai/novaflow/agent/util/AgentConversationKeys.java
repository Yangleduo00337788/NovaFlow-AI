package ai.novaflow.agent.util;

import org.springframework.util.StringUtils;

/**
 * Redis / 会话持久化共用的 conversation key 规则。
 */
public final class AgentConversationKeys {

    private AgentConversationKeys() {
    }

    public static String resolve(
            String requestedId,
            String prefix,
            Long tenantId,
            Long userId,
            Long agentId,
            String callerId
    ) {
        if (StringUtils.hasText(requestedId)) {
            return requestedId.trim();
        }
        String safePrefix = StringUtils.hasText(prefix) ? prefix.trim() : "chat";
        String scope;
        if (userId != null) {
            scope = "u" + userId;
        } else if (StringUtils.hasText(callerId)) {
            scope = "c-" + callerId.trim();
        } else {
            scope = "anon";
        }
        return safePrefix + "-" + tenantId + "-" + scope + "-" + agentId;
    }
}
