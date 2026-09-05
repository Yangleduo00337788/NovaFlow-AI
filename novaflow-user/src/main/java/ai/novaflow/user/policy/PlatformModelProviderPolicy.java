package ai.novaflow.user.policy;

import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.policy.ModelProviderPolicy;
import ai.novaflow.model.domain.ModelProviderPreset;
import ai.novaflow.user.service.PlatformSystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PlatformModelProviderPolicy implements ModelProviderPolicy {

    private final PlatformSystemConfigService platformSystemConfigService;

    @Override
    public void requireProviderAllowed(String providerCode) {
        Set<String> allowed = allowedProviderCodes();
        if (allowed.isEmpty()) {
            return;
        }
        String normalized = providerCode != null ? providerCode.trim().toLowerCase(Locale.ROOT) : "";
        if (!allowed.contains(normalized)) {
            String displayName = ModelProviderPreset.of(normalized)
                    .map(ModelProviderPreset::getName)
                    .orElse(normalized);
            throw new BusinessException("平台未开放模型供应商: " + displayName);
        }
    }

    @Override
    public Set<String> allowedProviderCodes() {
        return platformSystemConfigService.getAllowedProviderCodes();
    }
}
