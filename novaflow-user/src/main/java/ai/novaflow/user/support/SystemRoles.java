package ai.novaflow.user.support;

import com.mybatisflex.core.query.QueryWrapper;

/**
 * 系统角色查询。tenant_id=0 必须写在 SQL 字面量里，避免 ORM 把 0 当成空条件丢掉。
 */
public final class SystemRoles {

    private SystemRoles() {
    }

    public static QueryWrapper tenantSystem() {
        return QueryWrapper.create()
                .where("tenant_id = 0")
                .and("is_deleted = 0");
    }

    public static QueryWrapper byCode(String roleCode) {
        return tenantSystem().eq("role_code", roleCode);
    }
}
