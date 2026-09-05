package ai.novaflow.tenant.support;

import ai.novaflow.tenant.entity.TenantEntity;

/**
 * 新建租户时的套餐配额默认值（MyBatis 插入 null 会绕过数据库 DEFAULT）。
 */
public final class TenantLimits {

    private TenantLimits() {
    }

    public static void applyPlanDefaults(TenantEntity tenant) {
        if (tenant == null) {
            return;
        }
        String plan = tenant.getPlanType() != null ? tenant.getPlanType().trim().toLowerCase() : "free";
        switch (plan) {
            case "personal" -> applyPersonalDefaults(tenant);
            case "enterprise" -> applyEnterpriseDefaults(tenant);
            case "starter" -> applyStarterDefaults(tenant);
            case "professional", "pro" -> applyProfessionalDefaults(tenant);
            default -> applyFreeDefaults(tenant);
        }
    }

    private static void applyPersonalDefaults(TenantEntity tenant) {
        tenant.setMaxMembers(1);
        tenant.setMaxAgents(3);
        tenant.setMaxKnowledge(1);
        tenant.setMaxStorageMb(512);
        tenant.setMonthlyTokenQuota(20_000L);
    }

    private static void applyFreeDefaults(TenantEntity tenant) {
        tenant.setMaxMembers(10);
        tenant.setMaxAgents(5);
        tenant.setMaxKnowledge(3);
        tenant.setMaxStorageMb(1024);
        tenant.setMonthlyTokenQuota(100_000L);
    }

    private static void applyStarterDefaults(TenantEntity tenant) {
        tenant.setMaxMembers(15);
        tenant.setMaxAgents(10);
        tenant.setMaxKnowledge(5);
        tenant.setMaxStorageMb(2048);
        tenant.setMonthlyTokenQuota(300_000L);
    }

    private static void applyProfessionalDefaults(TenantEntity tenant) {
        tenant.setMaxMembers(30);
        tenant.setMaxAgents(20);
        tenant.setMaxKnowledge(10);
        tenant.setMaxStorageMb(5120);
        tenant.setMonthlyTokenQuota(1_000_000L);
    }

    private static void applyEnterpriseDefaults(TenantEntity tenant) {
        tenant.setMaxMembers(100);
        tenant.setMaxAgents(500);
        tenant.setMaxKnowledge(100);
        tenant.setMaxStorageMb(10240);
        tenant.setMonthlyTokenQuota(100_000_000L);
    }
}
