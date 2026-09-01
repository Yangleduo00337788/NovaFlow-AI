package ai.novaflow.server.integration;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 集成测试中临时将演示账号切换为平台超管角色，测试结束后恢复。
 */
public final class SuperAdminTestSupport {

    private static final long DEMO_MEMBER_ID = 1L;
    private static final long TENANT_ADMIN_ROLE_ID = 1L;
    private static final long SUPER_ADMIN_ROLE_ID = 2L;

    private SuperAdminTestSupport() {
    }

    public static void promoteDemoUserToSuperAdmin(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("UPDATE tenant_member SET role_id = ? WHERE id = ?", SUPER_ADMIN_ROLE_ID, DEMO_MEMBER_ID);
    }

    public static void restoreDemoUserTenantAdmin(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("UPDATE tenant_member SET role_id = ? WHERE id = ?", TENANT_ADMIN_ROLE_ID, DEMO_MEMBER_ID);
    }
}
