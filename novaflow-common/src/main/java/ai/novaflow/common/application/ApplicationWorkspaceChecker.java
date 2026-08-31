package ai.novaflow.common.application;

/**
 * 供组织管理等模块查询应用占用情况，避免 user 模块直接依赖 application。
 */
public interface ApplicationWorkspaceChecker {

    long countByWorkspace(Long workspaceId);
}
