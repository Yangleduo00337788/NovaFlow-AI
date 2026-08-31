package ai.novaflow.agent.service;

import ai.novaflow.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentOpenServiceSecurityTest {

    @Mock
    private OpenApiAuthService openApiAuthService;
    @Mock
    private AgentPublishService agentPublishService;
    @Mock
    private AgentService agentService;
    @Mock
    private AgentChatService agentChatService;
    @Mock
    private ai.novaflow.chat.service.ConversationService conversationService;

    @InjectMocks
    private AgentOpenService agentOpenService;

    @Test
    void embedTokenCannotListConversations() {
        when(openApiAuthService.authenticate(eq(1L), eq("nf_embed_secret")))
                .thenReturn(ai.novaflow.agent.domain.OpenApiAuthContext.builder()
                        .credentialType(ai.novaflow.agent.domain.OpenApiCredentialType.EMBED_TOKEN)
                        .tenantId(1L)
                        .agentId(1L)
                        .build());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> agentOpenService.listConversations(1L, "nf_embed_secret", "user-12345678", 1, 20));
        assertEquals(40303, ex.getCode());
    }
}
