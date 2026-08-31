package ai.novaflow.agent.service;

import ai.novaflow.agent.domain.OpenApiCredentialType;
import ai.novaflow.agent.entity.AgentApiKeyEntity;
import ai.novaflow.agent.entity.AgentEmbedTokenEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenApiAuthServiceTest {

    @Mock
    private AgentApiKeyService agentApiKeyService;
    @Mock
    private AgentEmbedTokenService agentEmbedTokenService;
    @InjectMocks
    private OpenApiAuthService openApiAuthService;

    @Test
    void authenticatesEmbedToken() {
        AgentEmbedTokenEntity entity = new AgentEmbedTokenEntity();
        entity.setTenantId(9L);
        when(agentEmbedTokenService.authenticate(eq(1L), eq("nf_embed_testtoken")))
                .thenReturn(entity);

        var context = openApiAuthService.authenticate(1L, "nf_embed_testtoken");

        assertEquals(OpenApiCredentialType.EMBED_TOKEN, context.credentialType());
        assertEquals(9L, context.tenantId());
    }

    @Test
    void authenticatesApiKey() {
        AgentApiKeyEntity entity = new AgentApiKeyEntity();
        entity.setTenantId(3L);
        when(agentApiKeyService.authenticate(eq(2L), eq("nf_live_testkey")))
                .thenReturn(entity);

        var context = openApiAuthService.authenticate(2L, "nf_live_testkey");

        assertEquals(OpenApiCredentialType.API_KEY, context.credentialType());
        assertEquals(3L, context.tenantId());
    }
}
