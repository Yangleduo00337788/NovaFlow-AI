package ai.novaflow.common.audit;

/**
 * 无操作实现，供单元测试或未装配审计模块时使用。
 */
public final class NoopAuditRecorder implements AuditRecorder {

    public static final NoopAuditRecorder INSTANCE = new NoopAuditRecorder();

    private NoopAuditRecorder() {
    }

    @Override
    public void record(String action, String resourceType, Long resourceId, String detail) {
    }

    @Override
    public void record(String action, String resourceType, Long resourceId, String detail, Long tenantId, Long userId) {
    }

    @Override
    public void record(String action, String resourceType, Long resourceId, String detail, Long tenantId, Long userId, String clientIp) {
    }
}
