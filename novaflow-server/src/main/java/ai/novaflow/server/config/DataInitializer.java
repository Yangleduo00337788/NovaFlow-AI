package ai.novaflow.server.config;

import ai.novaflow.agent.domain.AgentStatus;
import ai.novaflow.agent.entity.AgentConfigEntity;
import ai.novaflow.agent.entity.AgentEntity;
import ai.novaflow.agent.mapper.AgentConfigMapper;
import ai.novaflow.agent.mapper.AgentMapper;
import ai.novaflow.application.entity.ApplicationEntity;
import ai.novaflow.application.mapper.ApplicationMapper;
import ai.novaflow.tenant.entity.TenantEntity;
import ai.novaflow.tenant.entity.TenantMemberEntity;
import ai.novaflow.tenant.entity.WorkspaceEntity;
import ai.novaflow.tenant.mapper.TenantMapper;
import ai.novaflow.tenant.mapper.TenantMemberMapper;
import ai.novaflow.tenant.mapper.WorkspaceMapper;
import ai.novaflow.tenant.support.TenantLimits;
import ai.novaflow.user.entity.RoleEntity;
import ai.novaflow.user.entity.UserEntity;
import ai.novaflow.user.mapper.RoleMapper;
import ai.novaflow.user.mapper.UserMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 幂等初始化演示租户、角色演示账号与已发布应用（供门户使用）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String DEMO_TENANT_CODE = "demo";
    private static final int PUBLISH_STATUS_PUBLISHED = 1;

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final TenantMapper tenantMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final WorkspaceMapper workspaceMapper;
    private final ApplicationMapper applicationMapper;
    private final AgentMapper agentMapper;
    private final AgentConfigMapper agentConfigMapper;
    private final PasswordEncoder passwordEncoder;

    /** 演示数据开关：生产环境应设为 false（NOVAFLOW_DEMO_ENABLED=false） */
    @Value("${novaflow.demo.enabled:true}")
    private boolean demoEnabled;

    @Override
    public void run(String... args) {
        if (!demoEnabled) {
            log.info("Demo data initializer disabled (novaflow.demo.enabled=false)");
            return;
        }
        RoleEntity superAdminRole = requireSystemRole("super_admin");
        RoleEntity tenantOwnerRole = requireSystemRole("tenant_owner");
        RoleEntity developerRole = requireSystemRole("developer");
        RoleEntity operatorRole = requireSystemRole("operator");
        RoleEntity memberRole = requireSystemRole("member");
        RoleEntity viewerRole = requireSystemRole("viewer");
        if (superAdminRole == null || tenantOwnerRole == null || developerRole == null
                || operatorRole == null || memberRole == null || viewerRole == null) {
            log.warn("System roles not ready, skip demo account bootstrap");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        TenantEntity tenant = ensureDemoTenant(now);
        UserEntity adminUser = ensureDemoUser(
                "admin@novaflow.ai",
                "admin",
                "张三",
                "Admin123!",
                tenantOwnerRole,
                tenant,
                now
        );
        ensureDemoUser(
                "platform@novaflow.ai",
                "platform",
                "平台超管",
                "Platform123!",
                superAdminRole,
                tenant,
                now
        );
        ensureDemoUser(
                "user@novaflow.ai",
                "portaluser",
                "李四",
                "User123!",
                memberRole,
                tenant,
                now
        );
        ensureDemoUser(
                "developer@novaflow.ai",
                "developer",
                "王开发",
                "Developer123!",
                developerRole,
                tenant,
                now
        );
        ensureDemoUser(
                "operator@novaflow.ai",
                "operator",
                "赵运维",
                "Operator123!",
                operatorRole,
                tenant,
                now
        );
        ensureDemoUser(
                "viewer@novaflow.ai",
                "viewer",
                "钱只读",
                "Viewer123!",
                viewerRole,
                tenant,
                now
        );

        WorkspaceEntity workspace = ensureDefaultWorkspace(tenant, adminUser.getId(), now);
        ensurePublishedDemoApp(tenant, workspace, adminUser.getId(), now);

        log.info("""

                Demo accounts:
                  平台超管  platform@novaflow.ai
                  企业所有者 admin@novaflow.ai
                  开发者    developer@novaflow.ai
                  运维人员  operator@novaflow.ai
                  企业成员  user@novaflow.ai
                  只读用户  viewer@novaflow.ai
                  （演示入口 /login，密码见项目 README，请勿用于生产）
                """);
    }

    private RoleEntity requireSystemRole(String roleCode) {
        return roleMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", 0)
                        .eq("role_code", roleCode)
                        .eq("is_deleted", 0)
        );
    }

    private TenantEntity ensureDemoTenant(LocalDateTime now) {
        TenantEntity existing = tenantMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("tenant_code", DEMO_TENANT_CODE)
                        .eq("is_deleted", 0)
        );
        if (existing != null) {
            return existing;
        }

        TenantEntity tenant = new TenantEntity();
        tenant.setTenantCode(DEMO_TENANT_CODE);
        tenant.setTenantName("NovaFlow 演示企业");
        tenant.setPlanType("enterprise");
        tenant.setStatus(1);
        tenant.setExpireAt(LocalDateTime.of(2028, 12, 31, 23, 59, 59));
        TenantLimits.applyPlanDefaults(tenant);
        tenant.setIsDeleted(0);
        tenant.setCreatedAt(now);
        tenant.setUpdatedAt(now);
        tenantMapper.insert(tenant);
        return tenant;
    }

    private UserEntity ensureDemoUser(
            String email,
            String username,
            String nickname,
            String rawPassword,
            RoleEntity role,
            TenantEntity tenant,
            LocalDateTime now
    ) {
        UserEntity existing = userMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("email", email)
                        .eq("is_deleted", 0)
        );
        if (existing != null) {
            ensureMembership(existing, tenant, role, now);
            return existing;
        }

        UserEntity user = new UserEntity();
        user.setUsername(resolveUniqueUsername(username));
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setNickname(nickname);
        user.setStatus(1);
        user.setIsDeleted(0);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        ensureMembership(user, tenant, role, now);
        return user;
    }

    private void ensureMembership(UserEntity user, TenantEntity tenant, RoleEntity role, LocalDateTime now) {
        TenantMemberEntity member = tenantMemberMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenant.getId())
                        .eq("user_id", user.getId())
                        .eq("is_deleted", 0)
        );
        if (member != null) {
            if (!role.getId().equals(member.getRoleId())) {
                member.setRoleId(role.getId());
                member.setUpdatedAt(now);
                tenantMemberMapper.update(member);
            }
            return;
        }

        TenantMemberEntity newMember = new TenantMemberEntity();
        newMember.setTenantId(tenant.getId());
        newMember.setUserId(user.getId());
        newMember.setRoleId(role.getId());
        newMember.setStatus(1);
        newMember.setIsDeleted(0);
        newMember.setJoinedAt(now);
        newMember.setCreatedAt(now);
        newMember.setUpdatedAt(now);
        tenantMemberMapper.insert(newMember);
    }

    private String resolveUniqueUsername(String base) {
        String candidate = base;
        int suffix = 1;
        while (userMapper.selectCountByQuery(
                QueryWrapper.create().eq("username", candidate).eq("is_deleted", 0)
        ) > 0) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    private WorkspaceEntity ensureDefaultWorkspace(TenantEntity tenant, Long createdBy, LocalDateTime now) {
        WorkspaceEntity existing = workspaceMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenant.getId())
                        .eq("is_default", 1)
                        .eq("is_deleted", 0)
        );
        if (existing != null) {
            return existing;
        }

        WorkspaceEntity workspace = new WorkspaceEntity();
        workspace.setTenantId(tenant.getId());
        workspace.setWorkspaceName("默认工作空间");
        workspace.setIsDefault(1);
        workspace.setCreatedBy(createdBy);
        workspace.setIsDeleted(0);
        workspace.setCreatedAt(now);
        workspace.setUpdatedAt(now);
        workspaceMapper.insert(workspace);
        return workspace;
    }

    private void ensurePublishedDemoApp(
            TenantEntity tenant,
            WorkspaceEntity workspace,
            Long createdBy,
            LocalDateTime now
    ) {
        ApplicationEntity application = applicationMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenant.getId())
                        .eq("is_deleted", 0)
                        .orderBy("id", true)
                        .limit(1)
        );

        AgentEntity agent;
        if (application == null) {
            application = new ApplicationEntity();
            application.setTenantId(tenant.getId());
            application.setWorkspaceId(workspace.getId());
            application.setAppName("智能客服");
            application.setDescription("默认演示应用");
            application.setAppType("agent");
            application.setStatus(1);
            application.setPublishStatus(0);
            application.setCreatedBy(createdBy);
            application.setIsDeleted(0);
            application.setCreatedAt(now);
            application.setUpdatedAt(now);
            applicationMapper.insert(application);

            agent = new AgentEntity();
            agent.setTenantId(tenant.getId());
            agent.setApplicationId(application.getId());
            agent.setAgentName("智能客服 Agent");
            agent.setDescription("企业智能客服助手");
            agent.setAgentType("rag");
            agent.setStatus(AgentStatus.PUBLISHED);
            agent.setVersion(1);
            agent.setCreatedBy(createdBy);
            agent.setIsDeleted(0);
            agent.setCreatedAt(now);
            agent.setUpdatedAt(now);
            agentMapper.insert(agent);

            AgentConfigEntity config = new AgentConfigEntity();
            config.setTenantId(tenant.getId());
            config.setAgentId(agent.getId());
            config.setSystemPrompt("你是一个专业的企业智能客服助手，请礼貌、准确地回答用户问题。");
            config.setWelcomeMessage("您好，我是智能客服助手，有什么可以帮您？");
            config.setTemperature(new BigDecimal("0.70"));
            config.setMaxTokens(2048);
            config.setMemoryType("window");
            config.setMemoryWindow(10);
            config.setCreatedAt(now);
            config.setUpdatedAt(now);
            agentConfigMapper.insert(config);
        } else {
            agent = application.getDefaultAgentId() != null
                    ? agentMapper.selectOneById(application.getDefaultAgentId())
                    : null;
            if (agent == null) {
                agent = agentMapper.selectOneByQuery(
                        QueryWrapper.create()
                                .eq("tenant_id", tenant.getId())
                                .eq("application_id", application.getId())
                                .eq("is_deleted", 0)
                                .orderBy("id", true)
                                .limit(1)
                );
            }
            if (agent != null && agent.getStatus() != AgentStatus.PUBLISHED) {
                agent.setStatus(AgentStatus.PUBLISHED);
                agent.setUpdatedAt(now);
                agentMapper.update(agent);
            }
        }

        if (agent == null) {
            return;
        }

        boolean changed = false;
        if (!agent.getId().equals(application.getDefaultAgentId())) {
            application.setDefaultAgentId(agent.getId());
            changed = true;
        }
        if (application.getPublishStatus() == null || application.getPublishStatus() != PUBLISH_STATUS_PUBLISHED) {
            application.setPublishStatus(PUBLISH_STATUS_PUBLISHED);
            application.setPublishedAt(now);
            changed = true;
        }
        if (changed) {
            application.setUpdatedAt(now);
            applicationMapper.update(application);
        }
    }
}
