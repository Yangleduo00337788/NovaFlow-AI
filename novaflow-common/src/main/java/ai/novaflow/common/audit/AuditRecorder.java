package ai.novaflow.common.audit;

/**
 * 跨模块审计日志写入接口，由 novaflow-user 模块提供实现。
 */
public interface AuditRecorder {

    void record(String action, String resourceType, Long resourceId, String detail);

    void record(String action, String resourceType, Long resourceId, String detail, Long tenantId, Long userId);

    void record(String action, String resourceType, Long resourceId, String detail, Long tenantId, Long userId, String clientIp);
}
