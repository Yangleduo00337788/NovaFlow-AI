package ai.novaflow.server.search;

import ai.novaflow.agent.entity.AgentEntity;
import ai.novaflow.agent.mapper.AgentMapper;
import ai.novaflow.application.entity.ApplicationEntity;
import ai.novaflow.application.mapper.ApplicationMapper;
import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.security.PermissionCodes;
import ai.novaflow.common.security.ResourceTypes;
import ai.novaflow.knowledge.entity.KnowledgeBaseEntity;
import ai.novaflow.knowledge.mapper.KnowledgeBaseMapper;
import ai.novaflow.user.service.ResourceAccessService;
import ai.novaflow.workflow.entity.WorkflowEntity;
import ai.novaflow.workflow.mapper.WorkflowMapper;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GlobalSearchService {

    private static final int LIMIT_PER_TYPE = 5;

    private final ApplicationMapper applicationMapper;
    private final AgentMapper agentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final WorkflowMapper workflowMapper;
    private final ResourceAccessService resourceAccessService;

    public List<GlobalSearchItemVO> search(String keyword, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return List.of();
        }
        String q = keyword.trim();
        int perType = Math.max(1, Math.min(limit / 4, LIMIT_PER_TYPE));
        long userId = StpUtil.getLoginIdAsLong();

        List<GlobalSearchItemVO> items = new ArrayList<>();
        items.addAll(searchApplications(tenantId, userId, q, perType));
        items.addAll(searchAgents(tenantId, userId, q, perType));
        items.addAll(searchKnowledge(tenantId, userId, q, perType));
        items.addAll(searchWorkflows(tenantId, userId, q, perType));

        return items.stream()
                .sorted(Comparator.comparing(GlobalSearchItemVO::getTitle, String.CASE_INSENSITIVE_ORDER))
                .limit(Math.max(limit, 1))
                .toList();
    }

    private List<GlobalSearchItemVO> searchApplications(Long tenantId, long userId, String keyword, int limit) {
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", 0)
                .like("app_name", keyword)
                .orderBy("updated_at", false)
                .limit(limit);
        resourceAccessService.applyReadableFilter(
                query, userId, tenantId, ResourceTypes.APPLICATION, PermissionCodes.APPLICATION_READ, "application.id");
        return applicationMapper.selectListByQuery(query).stream().map(this::toApplicationItem).toList();
    }

    private List<GlobalSearchItemVO> searchAgents(Long tenantId, long userId, String keyword, int limit) {
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", 0)
                .like("agent_name", keyword)
                .orderBy("updated_at", false)
                .limit(limit);
        resourceAccessService.applyReadableFilter(
                query, userId, tenantId, ResourceTypes.AGENT, PermissionCodes.AGENT_READ, "agent.id");
        return agentMapper.selectListByQuery(query).stream().map(this::toAgentItem).toList();
    }

    private List<GlobalSearchItemVO> searchKnowledge(Long tenantId, long userId, String keyword, int limit) {
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", 0)
                .like("kb_name", keyword)
                .orderBy("updated_at", false)
                .limit(limit);
        resourceAccessService.applyReadableFilter(
                query, userId, tenantId, ResourceTypes.KNOWLEDGE, PermissionCodes.KNOWLEDGE_READ, "knowledge_base.id");
        return knowledgeBaseMapper.selectListByQuery(query).stream().map(this::toKnowledgeItem).toList();
    }

    private List<GlobalSearchItemVO> searchWorkflows(Long tenantId, long userId, String keyword, int limit) {
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", 0)
                .like("workflow_name", keyword)
                .orderBy("updated_at", false)
                .limit(limit);
        resourceAccessService.applyReadableFilter(
                query, userId, tenantId, ResourceTypes.WORKFLOW, PermissionCodes.WORKFLOW_READ, "workflow.id");
        return workflowMapper.selectListByQuery(query).stream().map(this::toWorkflowItem).toList();
    }

    private GlobalSearchItemVO toApplicationItem(ApplicationEntity entity) {
        return GlobalSearchItemVO.builder()
                .type("application")
                .id(entity.getId())
                .title(entity.getAppName())
                .subtitle("应用")
                .path("/application")
                .build();
    }

    private GlobalSearchItemVO toAgentItem(AgentEntity entity) {
        return GlobalSearchItemVO.builder()
                .type("agent")
                .id(entity.getId())
                .title(entity.getAgentName())
                .subtitle("Agent · " + entity.getAgentType())
                .path("/agent")
                .build();
    }

    private GlobalSearchItemVO toKnowledgeItem(KnowledgeBaseEntity entity) {
        return GlobalSearchItemVO.builder()
                .type("knowledge")
                .id(entity.getId())
                .title(entity.getKbName())
                .subtitle("知识库")
                .path("/knowledge/" + entity.getId())
                .build();
    }

    private GlobalSearchItemVO toWorkflowItem(WorkflowEntity entity) {
        return GlobalSearchItemVO.builder()
                .type("workflow")
                .id(entity.getId())
                .title(entity.getWorkflowName())
                .subtitle("工作流")
                .path("/workflow/" + entity.getId())
                .build();
    }
}
