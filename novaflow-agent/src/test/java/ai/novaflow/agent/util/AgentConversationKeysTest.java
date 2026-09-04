package ai.novaflow.agent.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentConversationKeysTest {

    @Test
    void usesExplicitConversationIdWhenProvided() {
        assertEquals("custom-key", AgentConversationKeys.resolve(
                "custom-key", "chat", 10L, 3L, 42L, null));
    }

    @Test
    void scopesDefaultKeyByTenantAndUser() {
        assertEquals("chat-10-u3-42", AgentConversationKeys.resolve(
                null, "chat", 10L, 3L, 42L, null));
    }

    @Test
    void scopesDefaultKeyByCallerWhenUserMissing() {
        assertEquals("open-10-c-enduser01-42", AgentConversationKeys.resolve(
                null, "open", 10L, null, 42L, "enduser01"));
    }

    @Test
    void differentUsersGetDifferentKeysForSameAgent() {
        String userA = AgentConversationKeys.resolve(null, "chat", 10L, 1L, 42L, null);
        String userB = AgentConversationKeys.resolve(null, "chat", 10L, 2L, 42L, null);
        assertEquals("chat-10-u1-42", userA);
        assertEquals("chat-10-u2-42", userB);
    }
}
