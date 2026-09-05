package ai.novaflow.common.security;

/**
 * 平台 IP 黑名单校验（由 novaflow-user 提供实现）。
 */
public interface IpBlacklistChecker {

    void requireAllowed(String clientIp);

    boolean isBlocked(String clientIp);
}
