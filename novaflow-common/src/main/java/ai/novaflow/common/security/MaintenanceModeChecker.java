package ai.novaflow.common.security;

/**
 * 平台维护模式开关（由 novaflow-user 实现，security 过滤器可选注入）。
 */
public interface MaintenanceModeChecker {

    int MAINTENANCE_CODE = 50301;

    boolean isMaintenanceEnabled();

    String getMaintenanceMessage();
}
