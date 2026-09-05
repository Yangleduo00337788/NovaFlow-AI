package ai.novaflow.model.policy;

import ai.novaflow.common.policy.ModelProviderPolicy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@ConditionalOnMissingBean(ModelProviderPolicy.class)
public class PermissiveModelProviderPolicy implements ModelProviderPolicy {

    @Override
    public void requireProviderAllowed(String providerCode) {
        // no-op
    }

    @Override
    public Set<String> allowedProviderCodes() {
        return Set.of();
    }
}
