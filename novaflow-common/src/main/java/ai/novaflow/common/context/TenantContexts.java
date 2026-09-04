package ai.novaflow.common.context;

import ai.novaflow.common.exception.BusinessException;

/**
 * 租户上下文辅助方法。
 */
public final class TenantContexts {

    private TenantContexts() {
    }

    public static Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户上下文缺失");
        }
        return tenantId;
    }
}
