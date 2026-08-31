package ai.novaflow.agent.service;

import ai.novaflow.agent.domain.OpenApiAuthContext;
import ai.novaflow.agent.domain.OpenApiCredentialType;
import ai.novaflow.agent.entity.AgentApiKeyEntity;
import ai.novaflow.agent.entity.AgentEmbedTokenEntity;
import ai.novaflow.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OpenApiAuthService {

    private final AgentApiKeyService agentApiKeyService;
    private final AgentEmbedTokenService agentEmbedTokenService;

    public OpenApiAuthContext authenticate(Long agentId, String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            throw new BusinessException(40101, "缺少 API Key 或 Embed Token");
        }
        String trimmed = rawToken.trim();
        if (trimmed.startsWith(AgentEmbedTokenService.TOKEN_PREFIX)) {
            AgentEmbedTokenEntity embedToken = agentEmbedTokenService.authenticate(agentId, trimmed);
            return OpenApiAuthContext.builder()
                    .credentialType(OpenApiCredentialType.EMBED_TOKEN)
                    .tenantId(embedToken.getTenantId())
                    .agentId(agentId)
                    .build();
        }
        AgentApiKeyEntity apiKey = agentApiKeyService.authenticate(agentId, trimmed);
        return OpenApiAuthContext.builder()
                .credentialType(OpenApiCredentialType.API_KEY)
                .tenantId(apiKey.getTenantId())
                .agentId(agentId)
                .build();
    }
}
