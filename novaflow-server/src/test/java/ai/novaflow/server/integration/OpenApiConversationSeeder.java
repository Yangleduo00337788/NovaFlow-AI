package ai.novaflow.server.integration;

import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

/**
 * 通过 SQL 写入 Open API 会话测试数据，避免在 E2E 测试中依赖 LLM 或额外 Bean。
 */
public final class OpenApiConversationSeeder {

    private OpenApiConversationSeeder() {
    }

    public static void seedConversation(
            JdbcTemplate jdbcTemplate,
            long tenantId,
            long agentId,
            String callerId,
            String conversationKey,
            String userMessage,
            String assistantReply) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                """
                        INSERT INTO conversation
                            (tenant_id, agent_id, conversation_key, channel, caller_id,
                             message_count, last_message_at, created_at, updated_at)
                        VALUES (?, ?, ?, 'open', ?, 2, ?, ?, ?)
                        """,
                tenantId,
                agentId,
                conversationKey,
                callerId,
                now,
                now,
                now
        );

        Long conversationId = jdbcTemplate.queryForObject(
                """
                        SELECT id FROM conversation
                        WHERE agent_id = ? AND conversation_key = ?
                        """,
                Long.class,
                agentId,
                conversationKey
        );
        if (conversationId == null) {
            throw new IllegalStateException("Failed to seed conversation: " + conversationKey);
        }

        jdbcTemplate.update(
                """
                        INSERT INTO conversation_message
                            (tenant_id, conversation_id, role, content, created_at)
                        VALUES (?, ?, 'user', ?, ?)
                        """,
                tenantId,
                conversationId,
                userMessage,
                now
        );
        jdbcTemplate.update(
                """
                        INSERT INTO conversation_message
                            (tenant_id, conversation_id, role, content, tokens_used, latency_ms, created_at)
                        VALUES (?, ?, 'assistant', ?, 1, 10, ?)
                        """,
                tenantId,
                conversationId,
                assistantReply,
                now
        );
    }
}
