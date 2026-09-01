package ai.novaflow.server.config;

import ai.novaflow.application.entity.ApplicationEntity;
import ai.novaflow.user.entity.RoleEntity;
import ai.novaflow.tenant.entity.TenantEntity;
import ai.novaflow.tenant.entity.TenantMemberEntity;
import ai.novaflow.user.entity.UserEntity;
import ai.novaflow.tenant.entity.WorkspaceEntity;
import ai.novaflow.tenant.support.TenantLimits;
import ai.novaflow.application.mapper.ApplicationMapper;
import ai.novaflow.user.mapper.RoleMapper;
import ai.novaflow.tenant.mapper.TenantMapper;
import ai.novaflow.tenant.mapper.TenantMemberMapper;
import ai.novaflow.user.mapper.UserMapper;
import ai.novaflow.tenant.mapper.WorkspaceMapper;
import ai.novaflow.agent.entity.AgentConfigEntity;
import ai.novaflow.agent.entity.AgentEntity;
import ai.novaflow.agent.mapper.AgentConfigMapper;
import ai.novaflow.agent.mapper.AgentMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final TenantMapper tenantMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final WorkspaceMapper workspaceMapper;
    private final ApplicationMapper applicationMapper;
    private final AgentMapper agentMapper;
    private final AgentConfigMapper agentConfigMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        long count = userMapper.selectCountByQuery(QueryWrapper.create());
        if (count > 0) {
            return;
        }
        log.info("Initializing demo data...");
        LocalDateTime now = LocalDateTime.now();

        RoleEntity adminRole = roleMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", 0)
                        .eq("role_code", "tenant_admin")
                        .eq("is_deleted", 0)
        );
        if (adminRole == null) {
            log.error("tenant_admin role not found, skip demo data initialization");
            return;
        }

        TenantEntity tenant = new TenantEntity();
        tenant.setTenantCode("demo");
        tenant.setTenantName("NovaFlow 演示企业");
        tenant.setPlanType("enterprise");
        tenant.setStatus(1);
        tenant.setExpireAt(LocalDateTime.of(2028, 12, 31, 23, 59, 59));
        TenantLimits.applyPlanDefaults(tenant);
        tenant.setIsDeleted(0);
        tenant.setCreatedAt(now);
        tenant.setUpdatedAt(now);
        tenantMapper.insert(tenant);

        UserEntity user = new UserEntity();
        user.setUsername("admin");
        user.setEmail("admin@novaflow.ai");
        user.setPasswordHash(passwordEncoder.encode("Admin123!"));
        user.setNickname("张三");
        user.setStatus(1);
        user.setIsDeleted(0);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);

        TenantMemberEntity member = new TenantMemberEntity();
        member.setTenantId(tenant.getId());
        member.setUserId(user.getId());
        member.setRoleId(adminRole.getId());
        member.setStatus(1);
        member.setIsDeleted(0);
        member.setJoinedAt(now);
        member.setCreatedAt(now);
        member.setUpdatedAt(now);
        tenantMemberMapper.insert(member);

        WorkspaceEntity workspace = new WorkspaceEntity();
        workspace.setTenantId(tenant.getId());
        workspace.setWorkspaceName("默认工作空间");
        workspace.setIsDefault(1);
        workspace.setCreatedBy(user.getId());
        workspace.setIsDeleted(0);
        workspace.setCreatedAt(now);
        workspace.setUpdatedAt(now);
        workspaceMapper.insert(workspace);

        ApplicationEntity application = new ApplicationEntity();
        application.setTenantId(tenant.getId());
        application.setWorkspaceId(workspace.getId());
        application.setAppName("智能客服");
        application.setDescription("默认演示应用");
        application.setAppType("agent");
        application.setStatus(1);
        application.setCreatedBy(user.getId());
        application.setIsDeleted(0);
        application.setCreatedAt(now);
        application.setUpdatedAt(now);
        applicationMapper.insert(application);

        AgentEntity agent = new AgentEntity();
        agent.setTenantId(tenant.getId());
        agent.setApplicationId(application.getId());
        agent.setAgentName("智能客服 Agent");
        agent.setDescription("企业智能客服助手");
        agent.setAgentType("rag");
        agent.setStatus(1);
        agent.setVersion(1);
        agent.setCreatedBy(user.getId());
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

        log.info("Demo data ready. Login: admin@novaflow.ai / Admin123!");
    }
}
