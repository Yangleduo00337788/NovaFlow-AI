package ai.novaflow.application.service;

import ai.novaflow.agent.domain.AgentStatus;
import ai.novaflow.agent.entity.AgentEntity;
import ai.novaflow.agent.mapper.AgentMapper;
import ai.novaflow.application.domain.dto.ApplicationSaveRequest;
import ai.novaflow.application.domain.vo.ApplicationPublishVO;
import ai.novaflow.application.domain.vo.ApplicationVO;
import ai.novaflow.common.audit.AuditRecorder;
import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.security.ResourceTypes;
import ai.novaflow.common.util.PageQueryUtils;
import ai.novaflow.knowledge.entity.KnowledgeBaseEntity;
import ai.novaflow.knowledge.mapper.KnowledgeBaseMapper;
import ai.novaflow.application.entity.ApplicationEntity;
import ai.novaflow.application.mapper.ApplicationMapper;
import ai.novaflow.tenant.entity.WorkspaceEntity;
import ai.novaflow.tenant.mapper.WorkspaceMapper;
import ai.novaflow.user.service.ResourceAccessService;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private static final int PUBLISH_STATUS_DRAFT = 0;
    private static final int PUBLISH_STATUS_PUBLISHED = 1;
    private static final int PUBLISH_STATUS_OFFLINE = 2;

    private final ApplicationMapper applicationMapper;
    private final WorkspaceMapper workspaceMapper;
    private final AgentMapper agentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final AuditRecorder auditRecorder;
    private final ResourceAccessService resourceAccessService;

    public PageResult<ApplicationVO> page(int page, int pageSize, String keyword) {
        page = PageQueryUtils.normalizePage(page);
        pageSize = PageQueryUtils.normalizePageSize(pageSize);
        Long tenantId = requireTenantId();
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", 0);
        if (StringUtils.hasText(keyword)) {
            query.and("(app_name like ? or description like ?)",
                    "%" + keyword + "%", "%" + keyword + "%");
        }
        query.orderBy("updated_at", false);

        Page<ApplicationEntity> result = applicationMapper.paginate(Page.of(page, pageSize), query);
        long userId = StpUtil.getLoginIdAsLong();
        List<ApplicationVO> list = result.getRecords().stream()
                .filter(entity -> resourceAccessService.canAccessResource(
                        userId, tenantId, ResourceTypes.APPLICATION, entity.getId(), "application:read"))
                .map(this::toSummaryVO)
                .toList();
        return PageResult.of(list, result.getTotalRow(), page, pageSize);
    }

    public List<ApplicationVO> listOptions() {
        Long tenantId = requireTenantId();
        long userId = StpUtil.getLoginIdAsLong();
        return applicationMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
                        .eq("status", 1)
                        .orderBy("app_name", true)
        ).stream()
                .filter(entity -> resourceAccessService.canAccessResource(
                        userId, tenantId, ResourceTypes.APPLICATION, entity.getId(), "application:read"))
                .map(this::toSummaryVO)
                .toList();
    }

    public ApplicationVO detail(Long id) {
        resourceAccessService.requireResourceAccess(
                StpUtil.getLoginIdAsLong(), requireTenantId(), ResourceTypes.APPLICATION, id, "application:read");
        ApplicationEntity entity = getApplicationOrThrow(id);
        return toDetailVO(entity);
    }

    @Transactional
    public ApplicationVO create(ApplicationSaveRequest request) {
        Long tenantId = requireTenantId();
        Long userId = StpUtil.getLoginIdAsLong();
        ensureNameUnique(tenantId, request.getAppName(), null);

        LocalDateTime now = LocalDateTime.now();
        ApplicationEntity entity = new ApplicationEntity();
        entity.setTenantId(tenantId);
        entity.setWorkspaceId(resolveDefaultWorkspaceId(tenantId));
        entity.setAppName(request.getAppName().trim());
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setIcon(trimToNull(request.getIcon()));
        entity.setAppType(StringUtils.hasText(request.getAppType()) ? request.getAppType() : "agent");
        entity.setAccessType(StringUtils.hasText(request.getAccessType()) ? request.getAccessType() : "team");
        entity.setPublishStatus(PUBLISH_STATUS_DRAFT);
        entity.setInvokeCount(0L);
        entity.setStatus(1);
        entity.setCreatedBy(userId);
        entity.setIsDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        applicationMapper.insert(entity);

        syncResources(entity, request);
        applicationMapper.update(entity);
        return toDetailVO(entity);
    }

    @Transactional
    public ApplicationVO update(Long id, ApplicationSaveRequest request) {
        resourceAccessService.requireResourceAccess(
                StpUtil.getLoginIdAsLong(), requireTenantId(), ResourceTypes.APPLICATION, id, "application:manage");
        ApplicationEntity entity = getApplicationOrThrow(id);
        ensureNameUnique(entity.getTenantId(), request.getAppName(), id);

        entity.setAppName(request.getAppName().trim());
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setIcon(trimToNull(request.getIcon()));
        if (StringUtils.hasText(request.getAppType())) {
            entity.setAppType(request.getAppType());
        }
        if (StringUtils.hasText(request.getAccessType())) {
            entity.setAccessType(request.getAccessType());
        }
        entity.setUpdatedAt(LocalDateTime.now());

        syncResources(entity, request);
        applicationMapper.update(entity);
        return toDetailVO(entity);
    }

    @Transactional
    public void delete(Long id) {
        resourceAccessService.requireResourceAccess(
                StpUtil.getLoginIdAsLong(), requireTenantId(), ResourceTypes.APPLICATION, id, "application:manage");
        ApplicationEntity entity = getApplicationOrThrow(id);
        if (countAgents(entity.getId(), entity.getTenantId()) > 0) {
            throw new BusinessException("应用下仍有关联 Agent，请先移除或迁移后再删除");
        }
        if (countKnowledgeBases(entity.getId(), entity.getTenantId()) > 0) {
            throw new BusinessException("应用下仍有关联知识库，请先移除或迁移后再删除");
        }
        entity.setIsDeleted(1);
        entity.setUpdatedAt(LocalDateTime.now());
        applicationMapper.update(entity);
        auditRecorder.record("application.delete", "application", entity.getId(), "删除应用: " + entity.getAppName());
    }

    public ApplicationPublishVO getPublishInfo(Long id) {
        resourceAccessService.requireResourceAccess(
                StpUtil.getLoginIdAsLong(), requireTenantId(), ResourceTypes.APPLICATION, id, "application:read");
        ApplicationEntity entity = getApplicationOrThrow(id);
        return buildPublishVO(entity);
    }

    @Transactional
    public ApplicationPublishVO publish(Long id) {
        resourceAccessService.requireResourceAccess(
                StpUtil.getLoginIdAsLong(), requireTenantId(), ResourceTypes.APPLICATION, id, "application:manage");
        ApplicationEntity entity = getApplicationOrThrow(id);
        if (entity.getDefaultAgentId() == null) {
            throw new BusinessException("发布前请设置默认入口 Agent");
        }
        AgentEntity defaultAgent = getAgentInApp(entity, entity.getDefaultAgentId());
        if (defaultAgent.getStatus() != AgentStatus.PUBLISHED) {
            throw new BusinessException("默认入口 Agent 尚未发布，请先在 Agent Studio 中发布");
        }

        entity.setPublishStatus(PUBLISH_STATUS_PUBLISHED);
        entity.setPublishedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        applicationMapper.update(entity);
        return buildPublishVO(entity);
    }

    @Transactional
    public ApplicationPublishVO unpublish(Long id) {
        resourceAccessService.requireResourceAccess(
                StpUtil.getLoginIdAsLong(), requireTenantId(), ResourceTypes.APPLICATION, id, "application:manage");
        ApplicationEntity entity = getApplicationOrThrow(id);
        entity.setPublishStatus(PUBLISH_STATUS_OFFLINE);
        entity.setUpdatedAt(LocalDateTime.now());
        applicationMapper.update(entity);
        return buildPublishVO(entity);
    }

    private void syncResources(ApplicationEntity entity, ApplicationSaveRequest request) {
        List<Long> agentIds = distinctIds(request.getAgentIds());
        List<Long> knowledgeBaseIds = distinctIds(request.getKnowledgeBaseIds());

        validateAgents(entity.getTenantId(), entity.getId(), agentIds);
        validateKnowledgeBases(entity.getTenantId(), knowledgeBaseIds);

        Long defaultAgentId = request.getDefaultAgentId();
        if (defaultAgentId != null && !agentIds.contains(defaultAgentId)) {
            throw new BusinessException("默认入口 Agent 必须包含在关联 Agent 列表中");
        }
        if (defaultAgentId == null && !agentIds.isEmpty()) {
            defaultAgentId = agentIds.get(0);
        }

        unbindRemovedAgents(entity, agentIds);
        bindAgents(entity.getTenantId(), entity.getId(), agentIds);
        bindKnowledgeBases(entity.getTenantId(), entity.getId(), knowledgeBaseIds);

        entity.setDefaultAgentId(defaultAgentId);
    }

    private void unbindRemovedAgents(ApplicationEntity entity, List<Long> keepAgentIds) {
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", entity.getTenantId())
                .eq("application_id", entity.getId())
                .eq("is_deleted", 0);
        if (!keepAgentIds.isEmpty()) {
            query.notIn("id", keepAgentIds);
        }
        List<AgentEntity> removed = agentMapper.selectListByQuery(query);
        if (removed.isEmpty()) {
            return;
        }
        Long fallbackAppId = resolveFallbackApplicationId(entity.getTenantId(), entity.getId());
        LocalDateTime now = LocalDateTime.now();
        for (AgentEntity agent : removed) {
            agent.setApplicationId(fallbackAppId);
            agent.setUpdatedAt(now);
            agentMapper.update(agent);
        }
    }

    private void bindAgents(Long tenantId, Long applicationId, List<Long> agentIds) {
        if (agentIds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Long agentId : agentIds) {
            AgentEntity agent = agentMapper.selectOneByQuery(
                    QueryWrapper.create()
                            .eq("id", agentId)
                            .eq("tenant_id", tenantId)
                            .eq("is_deleted", 0)
            );
            if (agent == null) {
                throw new BusinessException("Agent 不存在: " + agentId);
            }
            agent.setApplicationId(applicationId);
            agent.setUpdatedAt(now);
            agentMapper.update(agent);
        }
    }

    private void bindKnowledgeBases(Long tenantId, Long applicationId, List<Long> knowledgeBaseIds) {
        unbindRemovedKnowledgeBases(tenantId, applicationId, knowledgeBaseIds);
        if (knowledgeBaseIds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Long knowledgeBaseId : knowledgeBaseIds) {
            KnowledgeBaseEntity kb = knowledgeBaseMapper.selectOneByQuery(
                    QueryWrapper.create()
                            .eq("id", knowledgeBaseId)
                            .eq("tenant_id", tenantId)
                            .eq("is_deleted", 0)
            );
            if (kb == null) {
                throw new BusinessException("知识库不存在: " + knowledgeBaseId);
            }
            kb.setApplicationId(applicationId);
            kb.setUpdatedAt(now);
            knowledgeBaseMapper.update(kb);
        }
    }

    private void unbindRemovedKnowledgeBases(Long tenantId, Long applicationId, List<Long> keepIds) {
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("application_id", applicationId)
                .eq("is_deleted", 0);
        if (!keepIds.isEmpty()) {
            query.notIn("id", keepIds);
        }
        List<KnowledgeBaseEntity> removed = knowledgeBaseMapper.selectListByQuery(query);
        if (removed.isEmpty()) {
            return;
        }
        Long fallbackAppId = resolveFallbackApplicationId(tenantId, applicationId);
        LocalDateTime now = LocalDateTime.now();
        for (KnowledgeBaseEntity kb : removed) {
            kb.setApplicationId(fallbackAppId);
            kb.setUpdatedAt(now);
            knowledgeBaseMapper.update(kb);
        }
    }

    private void validateAgents(Long tenantId, Long applicationId, List<Long> agentIds) {
        for (Long agentId : agentIds) {
            AgentEntity agent = agentMapper.selectOneByQuery(
                    QueryWrapper.create()
                            .eq("id", agentId)
                            .eq("tenant_id", tenantId)
                            .eq("is_deleted", 0)
            );
            if (agent == null) {
                throw new BusinessException("Agent 不存在: " + agentId);
            }
        }
    }

    private void validateKnowledgeBases(Long tenantId, List<Long> knowledgeBaseIds) {
        for (Long knowledgeBaseId : knowledgeBaseIds) {
            KnowledgeBaseEntity kb = knowledgeBaseMapper.selectOneByQuery(
                    QueryWrapper.create()
                            .eq("id", knowledgeBaseId)
                            .eq("tenant_id", tenantId)
                            .eq("is_deleted", 0)
            );
            if (kb == null) {
                throw new BusinessException("知识库不存在: " + knowledgeBaseId);
            }
        }
    }

    private AgentEntity getAgentInApp(ApplicationEntity entity, Long agentId) {
        AgentEntity agent = agentMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", agentId)
                        .eq("tenant_id", entity.getTenantId())
                        .eq("application_id", entity.getId())
                        .eq("is_deleted", 0)
        );
        if (agent == null) {
            throw new BusinessException("默认入口 Agent 不属于当前应用");
        }
        return agent;
    }

    private ApplicationPublishVO buildPublishVO(ApplicationEntity entity) {
        String defaultAgentName = null;
        if (entity.getDefaultAgentId() != null) {
            AgentEntity agent = agentMapper.selectOneById(entity.getDefaultAgentId());
            defaultAgentName = agent != null ? agent.getAgentName() : null;
        }
        ApplicationPublishVO.ApplicationPublishVOBuilder builder = ApplicationPublishVO.builder()
                .applicationId(entity.getId())
                .publishStatus(entity.getPublishStatus())
                .defaultAgentId(entity.getDefaultAgentId())
                .defaultAgentName(defaultAgentName)
                .publishedAt(entity.getPublishedAt());
        if (entity.getDefaultAgentId() != null) {
            Long agentId = entity.getDefaultAgentId();
            builder.chatEndpoint("/api/v1/open/agents/" + agentId + "/chat")
                    .streamEndpoint("/api/v1/open/agents/" + agentId + "/chat/stream")
                    .embedPath("/embed/agents/" + agentId)
                    .portalPath(PortalService.buildPortalPath(entity.getId()));
        }
        return builder.build();
    }

    private ApplicationVO toSummaryVO(ApplicationEntity entity) {
        List<Long> agentIds = listAgentIds(entity.getId(), entity.getTenantId());
        List<Long> knowledgeBaseIds = listKnowledgeBaseIds(entity.getId(), entity.getTenantId());
        return ApplicationVO.builder()
                .id(entity.getId())
                .workspaceId(entity.getWorkspaceId())
                .appName(entity.getAppName())
                .description(entity.getDescription())
                .icon(entity.getIcon())
                .appType(entity.getAppType())
                .defaultAgentId(entity.getDefaultAgentId())
                .defaultAgentName(resolveAgentName(entity.getDefaultAgentId()))
                .publishStatus(entity.getPublishStatus())
                .accessType(entity.getAccessType())
                .invokeCount(entity.getInvokeCount())
                .publishedAt(entity.getPublishedAt())
                .status(entity.getStatus())
                .agentCount(agentIds.size())
                .knowledgeBaseCount(knowledgeBaseIds.size())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ApplicationVO toDetailVO(ApplicationEntity entity) {
        List<Long> agentIds = listAgentIds(entity.getId(), entity.getTenantId());
        List<Long> knowledgeBaseIds = listKnowledgeBaseIds(entity.getId(), entity.getTenantId());
        return ApplicationVO.builder()
                .id(entity.getId())
                .workspaceId(entity.getWorkspaceId())
                .appName(entity.getAppName())
                .description(entity.getDescription())
                .icon(entity.getIcon())
                .appType(entity.getAppType())
                .defaultAgentId(entity.getDefaultAgentId())
                .defaultAgentName(resolveAgentName(entity.getDefaultAgentId()))
                .publishStatus(entity.getPublishStatus())
                .accessType(entity.getAccessType())
                .invokeCount(entity.getInvokeCount())
                .publishedAt(entity.getPublishedAt())
                .status(entity.getStatus())
                .agentCount(agentIds.size())
                .knowledgeBaseCount(knowledgeBaseIds.size())
                .agentIds(agentIds)
                .knowledgeBaseIds(knowledgeBaseIds)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private List<Long> listAgentIds(Long applicationId, Long tenantId) {
        return agentMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("application_id", applicationId)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
                        .orderBy("updated_at", false)
        ).stream().map(AgentEntity::getId).toList();
    }

    private List<Long> listKnowledgeBaseIds(Long applicationId, Long tenantId) {
        return knowledgeBaseMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("application_id", applicationId)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
                        .orderBy("updated_at", false)
        ).stream().map(KnowledgeBaseEntity::getId).toList();
    }

    private int countAgents(Long applicationId, Long tenantId) {
        return (int) agentMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq("application_id", applicationId)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
        );
    }

    private int countKnowledgeBases(Long applicationId, Long tenantId) {
        return (int) knowledgeBaseMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq("application_id", applicationId)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
        );
    }

    private String resolveAgentName(Long agentId) {
        if (agentId == null) {
            return null;
        }
        AgentEntity agent = agentMapper.selectOneById(agentId);
        return agent != null ? agent.getAgentName() : null;
    }

    private Long resolveDefaultWorkspaceId(Long tenantId) {
        WorkspaceEntity workspace = workspaceMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("is_default", 1)
                        .eq("is_deleted", 0)
                        .limit(1)
        );
        if (workspace == null) {
            workspace = workspaceMapper.selectOneByQuery(
                    QueryWrapper.create()
                            .eq("tenant_id", tenantId)
                            .eq("is_deleted", 0)
                            .orderBy("id", true)
                            .limit(1)
            );
        }
        if (workspace == null) {
            throw new BusinessException("未找到可用工作空间");
        }
        return workspace.getId();
    }

    private Long resolveFallbackApplicationId(Long tenantId, Long excludeId) {
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", 0)
                .orderBy("id", true)
                .limit(1);
        if (excludeId != null) {
            query.ne("id", excludeId);
        }
        ApplicationEntity fallback = applicationMapper.selectOneByQuery(query);
        if (fallback == null) {
            throw new BusinessException("无法找到备用应用用于资源迁移");
        }
        return fallback.getId();
    }

    private ApplicationEntity getApplicationOrThrow(Long id) {
        ApplicationEntity entity = applicationMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", id)
                        .eq("tenant_id", requireTenantId())
                        .eq("is_deleted", 0)
        );
        if (entity == null) {
            throw new BusinessException("应用不存在");
        }
        return entity;
    }

    private void ensureNameUnique(Long tenantId, String appName, Long excludeId) {
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("app_name", appName.trim())
                .eq("is_deleted", 0);
        if (excludeId != null) {
            query.ne("id", excludeId);
        }
        if (applicationMapper.selectCountByQuery(query) > 0) {
            throw new BusinessException("应用名称已存在");
        }
    }

    private List<Long> distinctIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toCollection(ArrayList::new));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户上下文缺失");
        }
        return tenantId;
    }
}
