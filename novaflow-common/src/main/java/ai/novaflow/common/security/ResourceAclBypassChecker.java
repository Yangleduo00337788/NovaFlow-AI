package ai.novaflow.common.security;

/**
 * 判断用户是否可绕过资源级 ACL（如 Owner/Admin）。
 */
public interface ResourceAclBypassChecker {

    boolean bypassesResourceAcl(long userId, Long tenantId);
}
