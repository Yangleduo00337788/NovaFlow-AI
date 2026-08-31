package ai.novaflow.agent.domain;

import lombok.Builder;

@Builder
public record OpenApiAuthContext(
        OpenApiCredentialType credentialType,
        Long tenantId,
        Long agentId
) {
}
