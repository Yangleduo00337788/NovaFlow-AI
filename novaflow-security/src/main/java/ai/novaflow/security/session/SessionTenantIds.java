package ai.novaflow.security.session;

/**
 * Redis/Jackson 反序列化后 session 里的 tenantId 可能是 Integer、Long 或其它 Number。
 */
public final class SessionTenantIds {

    private SessionTenantIds() {
    }

    public static Long toLong(Object tenantId) {
        if (tenantId instanceof Long id) {
            return id;
        }
        if (tenantId instanceof Integer id) {
            return id.longValue();
        }
        if (tenantId instanceof Number number) {
            return number.longValue();
        }
        if (tenantId instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
