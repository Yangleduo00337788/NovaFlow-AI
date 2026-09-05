package ai.novaflow.server.integration;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 集成测试中临时撤销 viewer 角色的 portal:access，测试结束后恢复。
 */
public final class PortalAccessTestSupport {

    private PortalAccessTestSupport() {
    }

    public static void revokeViewerPortalAccess(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("""
                DELETE rp FROM role_permission rp
                INNER JOIN role r ON r.id = rp.role_id AND r.tenant_id = 0 AND r.role_code = 'viewer'
                INNER JOIN permission p ON p.id = rp.permission_id AND p.permission_code = 'portal:access'
                """);
    }

    public static void restoreViewerPortalAccess(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("""
                INSERT IGNORE INTO role_permission (role_id, permission_id)
                SELECT r.id, p.id
                FROM role r
                JOIN permission p ON p.permission_code = 'portal:access'
                WHERE r.tenant_id = 0 AND r.role_code = 'viewer'
                """);
    }
}
