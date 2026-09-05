package ai.novaflow.common.policy;

import java.util.Set;

/**
 * 平台级模型供应商策略（由 novaflow-user 提供实现）。
 */
public interface ModelProviderPolicy {

    void requireProviderAllowed(String providerCode);

    /**
     * @return 空集合表示未启用白名单（全部允许）
     */
    Set<String> allowedProviderCodes();
}
