package ai.novaflow.agent.service;

import ai.novaflow.agent.domain.dto.AgentSaveRequest;
import ai.novaflow.agent.domain.vo.AgentVO;
import ai.novaflow.agent.entity.AgentConfigEntity;
import ai.novaflow.agent.entity.AgentEntity;
import ai.novaflow.agent.entity.AgentKnowledgeEntity;
import ai.novaflow.agent.entity.AgentSkillEntity;
import ai.novaflow.agent.entity.AgentToolEntity;
import ai.novaflow.agent.mapper.AgentConfigMapper;
import ai.novaflow.agent.mapper.AgentKnowledgeMapper;
import ai.novaflow.agent.mapper.AgentMapper;
import ai.novaflow.agent.mapper.AgentSkillMapper;
import ai.novaflow.agent.mapper.AgentToolMapper;
import ai.novaflow.common.audit.AuditRecorder;
import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.domain.RetrievalConfig;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.agent.util.AgentExtraConfigUtils;
import ai.novaflow.common.util.RetrievalConfigUtils;
import ai.novaflow.prompt.service.PromptTemplateService;
import ai.novaflow.tool.service.ToolDefinitionService;
import ai.novaflow.workflow.entity.WorkflowEntity;
import ai.novaflow.workflow.service.WorkflowService;
import ai.novaflow.tool.domain.HttpToolDefinition;
import ai.novaflow.user.service.RecentAccessService;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentMapper agentMapper;
    private final AgentConfigMapper agentConfigMapper;
    private final AgentKnowledgeMapper agentKnowledgeMapper;
    private final AgentToolMapper agentToolMapper;
    private final AgentSkillMapper agentSkillMapper;
    private final ToolDefinitionService toolDefinitionService;
    private final PromptTemplateService promptTemplateService;
    private final RecentAccessService recentAccessService;
    private final WorkflowService workflowService;
    private final ObjectMapper objectMapper;
    private final AuditRecorder auditRecorder;

    public PageResult<AgentVO> page(int page, int pageSize, String keyword, String agentType) {
        Long tenantId = requireTenantId();
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", 0);
        if (StringUtils.hasText(keyword)) {
            query.like("agent_name", keyword);
        }
        if (StringUtils.hasText(agentType)) {
            query.eq("agent_type", agentType);
        }
        query.orderBy("updated_at", false);

        Page<AgentEntity> result = agentMapper.paginate(Page.of(page, pageSize), query);
        List<AgentVO> list = result.getRecords().stream().map(this::toSimpleVO).toList();
        return PageResult.of(list, result.getTotalRow(), page, pageSize);
    }

    public AgentVO detail(Long id) {
        return detail(id, true);
    }

    public AgentVO detailWithoutAccessRecord(Long id) {
        return detail(id, false);
    }

    private AgentVO detail(Long id, boolean recordAccess) {
        AgentEntity agent = getAgentOrThrow(id);
        AgentConfigEntity config = agentConfigMapper.selectOneByQuery(
                QueryWrapper.create().eq("agent_id", id).limit(1)
        );
        if (recordAccess) {
            recordRecentAccess(agent);
        }
        return toDetailVO(agent, config);
    }

    @Transactional
    public AgentVO create(AgentSaveRequest request) {
        Long tenantId = requireTenantId();
        Long userId = StpUtil.getLoginIdAsLong();
        if (request.getApplicationId() == null) {
            throw new BusinessException("请选择所属应用");
        }
        Long applicationId = request.getApplicationId();

        AgentEntity agent = new AgentEntity();
        agent.setTenantId(tenantId);
        agent.setApplicationId(applicationId);
        agent.setAgentName(request.getAgentName());
        agent.setDescription(request.getDescription());
        agent.setIcon(request.getIcon());
        agent.setAgentType(request.getAgentType());
        agent.setStatus(0);
        validateWorkflowBinding(request, tenantId);
        agent.setVersion(1);
        agent.setCreatedBy(userId);
        agent.setIsDeleted(0);
        agent.setCreatedAt(LocalDateTime.now());
        agent.setUpdatedAt(LocalDateTime.now());
        agentMapper.insert(agent);

        AgentConfigEntity config = buildConfig(agent.getId(), tenantId, request);
        agentConfigMapper.insert(config);
        saveKnowledgeBindings(agent.getId(), tenantId, request.getKnowledgeBaseIds());
        saveToolBindings(agent.getId(), tenantId, request.getToolIds());
        saveSkillBindings(agent.getId(), tenantId, request.getSkillIds());

        return toDetailVO(agent, config);
    }

    @Transactional
    public AgentVO update(Long id, AgentSaveRequest request) {
        AgentEntity agent = getAgentOrThrow(id);
        agent.setAgentName(request.getAgentName());
        agent.setDescription(request.getDescription());
        agent.setIcon(request.getIcon());
        agent.setAgentType(request.getAgentType());
        validateWorkflowBinding(request, agent.getTenantId());
        agent.setUpdatedAt(LocalDateTime.now());
        agentMapper.update(agent);

        AgentConfigEntity config = agentConfigMapper.selectOneByQuery(
                QueryWrapper.create().eq("agent_id", id).limit(1)
        );
        if (config == null) {
            config = buildConfig(id, agent.getTenantId(), request);
            agentConfigMapper.insert(config);
        } else {
            applyConfig(config, request);
            config.setUpdatedAt(LocalDateTime.now());
            agentConfigMapper.update(config);
        }
        saveKnowledgeBindings(id, agent.getTenantId(), request.getKnowledgeBaseIds());
        saveToolBindings(id, agent.getTenantId(), request.getToolIds());
        saveSkillBindings(id, agent.getTenantId(), request.getSkillIds());
        return toDetailVO(agent, config);
    }

    @Transactional
    public void delete(Long id) {
        AgentEntity agent = getAgentOrThrow(id);
        agent.setIsDeleted(1);
        agent.setUpdatedAt(LocalDateTime.now());
        agentMapper.update(agent);
        auditRecorder.record("agent.delete", "agent", agent.getId(), "删除 Agent: " + agent.getAgentName());
    }

    public List<Long> listKnowledgeBaseIds(Long agentId) {
        return agentKnowledgeMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("agent_id", agentId)
                        .eq("tenant_id", requireTenantId())
        ).stream().map(AgentKnowledgeEntity::getKnowledgeBaseId).toList();
    }

    private void saveToolBindings(Long agentId, Long tenantId, List<Long> toolIds) {
        agentToolMapper.deleteByQuery(
                QueryWrapper.create()
                        .eq("agent_id", agentId)
                        .eq("tenant_id", tenantId)
        );
        if (toolIds == null || toolIds.isEmpty()) {
            return;
        }
        List<Long> uniqueIds = toolIds.stream().distinct().toList();
        LocalDateTime now = LocalDateTime.now();
        int sortOrder = 0;
        for (Long toolId : uniqueIds) {
            toolDefinitionService.ensureCallableTool(toolId);
            AgentToolEntity binding = new AgentToolEntity();
            binding.setAgentId(agentId);
            binding.setTenantId(tenantId);
            binding.setToolId(toolId);
            binding.setSortOrder(sortOrder++);
            binding.setCreatedAt(now);
            agentToolMapper.insert(binding);
        }
    }

    private List<Long> loadToolIds(Long agentId, Long tenantId) {
        return agentToolMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("agent_id", agentId)
                        .eq("tenant_id", tenantId)
                        .orderBy("sort_order", true)
        ).stream().map(AgentToolEntity::getToolId).toList();
    }

    private void saveSkillBindings(Long agentId, Long tenantId, List<Long> skillIds) {
        agentSkillMapper.deleteByQuery(
                QueryWrapper.create()
                        .eq("agent_id", agentId)
                        .eq("tenant_id", tenantId)
        );
        if (skillIds == null || skillIds.isEmpty()) {
            return;
        }
        List<Long> uniqueIds = skillIds.stream().distinct().toList();
        LocalDateTime now = LocalDateTime.now();
        int sortOrder = 0;
        for (Long skillId : uniqueIds) {
            toolDefinitionService.ensureSkill(skillId);
            AgentSkillEntity binding = new AgentSkillEntity();
            binding.setAgentId(agentId);
            binding.setTenantId(tenantId);
            binding.setSkillId(skillId);
            binding.setSortOrder(sortOrder++);
            binding.setCreatedAt(now);
            agentSkillMapper.insert(binding);
        }
    }

    private List<Long> loadSkillIds(Long agentId, Long tenantId) {
        return agentSkillMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("agent_id", agentId)
                        .eq("tenant_id", tenantId)
                        .orderBy("sort_order", true)
        ).stream().map(AgentSkillEntity::getSkillId).toList();
    }

    private List<HttpToolDefinition> resolveAgentTools(Long agentId, Long tenantId, String extraConfig) {
        List<HttpToolDefinition> marketplaceTools = toolDefinitionService.resolveTools(
                tenantId,
                loadToolIds(agentId, tenantId)
        );
        List<HttpToolDefinition> inlineTools = AgentExtraConfigUtils.parseTools(objectMapper, extraConfig);
        if (marketplaceTools.isEmpty()) {
            return inlineTools;
        }
        if (inlineTools.isEmpty()) {
            return marketplaceTools;
        }
        Map<String, HttpToolDefinition> merged = new LinkedHashMap<>();
        for (HttpToolDefinition tool : marketplaceTools) {
            if (tool.getName() != null) {
                merged.put(tool.getName(), tool);
            }
        }
        for (HttpToolDefinition tool : inlineTools) {
            if (tool.getName() != null && !merged.containsKey(tool.getName())) {
                merged.put(tool.getName(), tool);
            }
        }
        return new ArrayList<>(merged.values());
    }

    private void saveKnowledgeBindings(Long agentId, Long tenantId, List<Long> knowledgeBaseIds) {
        agentKnowledgeMapper.deleteByQuery(
                QueryWrapper.create()
                        .eq("agent_id", agentId)
                        .eq("tenant_id", tenantId)
        );
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            return;
        }
        List<Long> uniqueIds = knowledgeBaseIds.stream().distinct().toList();
        LocalDateTime now = LocalDateTime.now();
        for (Long knowledgeBaseId : uniqueIds) {
            AgentKnowledgeEntity binding = new AgentKnowledgeEntity();
            binding.setAgentId(agentId);
            binding.setTenantId(tenantId);
            binding.setKnowledgeBaseId(knowledgeBaseId);
            binding.setCreatedAt(now);
            agentKnowledgeMapper.insert(binding);
        }
    }

    private List<Long> loadKnowledgeBaseIds(Long agentId, Long tenantId) {
        return agentKnowledgeMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("agent_id", agentId)
                        .eq("tenant_id", tenantId)
        ).stream().map(AgentKnowledgeEntity::getKnowledgeBaseId).toList();
    }

    public AgentEntity getAgentEntityOrThrow(Long id) {
        return getAgentOrThrow(id);
    }

    private AgentEntity getAgentOrThrow(Long id) {
        AgentEntity agent = agentMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", id)
                        .eq("tenant_id", requireTenantId())
                        .eq("is_deleted", 0)
        );
        if (agent == null) {
            throw new BusinessException("Agent不存在");
        }
        return agent;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户上下文缺失");
        }
        return tenantId;
    }

    private AgentConfigEntity buildConfig(Long agentId, Long tenantId, AgentSaveRequest request) {
        AgentConfigEntity config = new AgentConfigEntity();
        config.setAgentId(agentId);
        config.setTenantId(tenantId);
        applyConfig(config, request);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        return config;
    }

    private void applyConfig(AgentConfigEntity config, AgentSaveRequest request) {
        applyPromptConfig(config, request);
        config.setWelcomeMessage(request.getWelcomeMessage());
        config.setModelConfigId(request.getModelConfigId());
        config.setTemperature(request.getTemperature());
        config.setMaxTokens(request.getMaxTokens());
        config.setMemoryType(request.getMemoryType());
        config.setMemoryWindow(request.getMemoryWindow());
        config.setWorkflowId("workflow".equals(request.getAgentType()) ? request.getWorkflowId() : null);
        config.setRetrievalConfig(RetrievalConfigUtils.serialize(objectMapper, toRetrievalConfig(request)));
        config.setExtraConfig(AgentExtraConfigUtils.serializeTools(objectMapper, request.getTools()));
    }

    private void applyPromptConfig(AgentConfigEntity config, AgentSaveRequest request) {
        String refMode = StringUtils.hasText(request.getPromptRefMode()) ? request.getPromptRefMode().trim() : null;
        Long templateId = request.getPromptTemplateId();

        if (templateId == null) {
            config.setPromptTemplateId(null);
            config.setPromptTemplateVersionId(null);
            config.setPromptRefMode(null);
            config.setSystemPrompt(StringUtils.hasText(request.getSystemPrompt())
                    ? request.getSystemPrompt().trim()
                    : null);
            return;
        }

        String templateContent = promptTemplateService.resolveContent(
                config.getTenantId(),
                templateId,
                request.getPromptTemplateVersionId()
        );
        promptTemplateService.incrementUsageCount(templateId);

        config.setPromptTemplateId(templateId);
        config.setPromptRefMode(refMode);

        if ("reference".equals(refMode)) {
            config.setPromptTemplateVersionId(null);
            config.setSystemPrompt(null);
            return;
        }

        config.setPromptTemplateVersionId(request.getPromptTemplateVersionId());
        if (StringUtils.hasText(request.getSystemPrompt())) {
            config.setSystemPrompt(request.getSystemPrompt().trim());
        } else {
            config.setSystemPrompt(templateContent);
        }
        if (!StringUtils.hasText(refMode)) {
            config.setPromptRefMode("copy");
        }
    }

    private String resolveSystemPrompt(AgentConfigEntity config) {
        if (config == null) {
            return null;
        }
        if ("reference".equals(config.getPromptRefMode()) && config.getPromptTemplateId() != null) {
            String content = promptTemplateService.resolveRenderedContent(
                    config.getTenantId(),
                    config.getPromptTemplateId(),
                    null
            );
            return StringUtils.hasText(content) ? content.trim() : null;
        }
        if (StringUtils.hasText(config.getSystemPrompt())) {
            String raw = config.getSystemPrompt().trim();
            if (config.getPromptTemplateId() != null) {
                return promptTemplateService.renderWithTemplateDefaults(
                        config.getTenantId(),
                        config.getPromptTemplateId(),
                        config.getPromptTemplateVersionId(),
                        raw
                );
            }
            return raw;
        }
        return null;
    }

    public String resolveRuntimeSystemPrompt(AgentVO agent, Long tenantId) {
        if (agent == null || tenantId == null) {
            return null;
        }
        if ("reference".equals(agent.getPromptRefMode()) && agent.getPromptTemplateId() != null) {
            String content = promptTemplateService.resolveRenderedContent(
                    tenantId,
                    agent.getPromptTemplateId(),
                    null
            );
            return StringUtils.hasText(content) ? content.trim() : null;
        }
        if (StringUtils.hasText(agent.getSystemPrompt())) {
            String raw = agent.getSystemPrompt().trim();
            if (agent.getPromptTemplateId() != null) {
                return promptTemplateService.renderWithTemplateDefaults(
                        tenantId,
                        agent.getPromptTemplateId(),
                        agent.getPromptTemplateVersionId(),
                        raw
                );
            }
            return raw;
        }
        return null;
    }

    public String resolveFullSystemPrompt(AgentVO agent, Long tenantId) {
        String basePrompt = resolveRuntimeSystemPrompt(agent, tenantId);
        String skillBlock = toolDefinitionService.buildSkillSystemPromptBlock(
                tenantId,
                agent != null ? agent.getSkillIds() : null
        );
        if (!StringUtils.hasText(skillBlock)) {
            return basePrompt;
        }
        if (!StringUtils.hasText(basePrompt)) {
            return skillBlock;
        }
        return basePrompt.trim() + "\n\n" + skillBlock.trim();
    }

    private RetrievalConfig toRetrievalConfig(AgentSaveRequest request) {
        return RetrievalConfig.builder()
                .topK(request.getRetrievalTopK())
                .scoreThreshold(request.getRetrievalScoreThreshold())
                .rerankEnabled(request.getRerankEnabled())
                .rerankModel(request.getRerankModel())
                .rerankCandidateK(request.getRerankCandidateK())
                .hybridEnabled(request.getHybridEnabled())
                .hybridAlpha(request.getHybridAlpha())
                .build();
    }

    private void validateWorkflowBinding(AgentSaveRequest request, Long tenantId) {
        if (!"workflow".equals(request.getAgentType())) {
            return;
        }
        if (request.getWorkflowId() == null) {
            throw new BusinessException("Workflow Agent 需选择工作流");
        }
        workflowService.requireWorkflow(request.getWorkflowId(), tenantId);
    }

    private void recordRecentAccess(AgentEntity agent) {
        try {
            if (!StpUtil.isLogin()) {
                return;
            }
            recentAccessService.record(
                    agent.getTenantId(),
                    StpUtil.getLoginIdAsLong(),
                    "agent",
                    agent.getId(),
                    agent.getAgentName()
            );
        } catch (Exception ignored) {
            // Open API 异步线程等非 Web 上下文下跳过最近访问记录
        }
    }

    private AgentVO toSimpleVO(AgentEntity agent) {
        return AgentVO.builder()
                .id(agent.getId())
                .applicationId(agent.getApplicationId())
                .agentName(agent.getAgentName())
                .description(agent.getDescription())
                .icon(agent.getIcon())
                .agentType(agent.getAgentType())
                .status(agent.getStatus())
                .version(agent.getVersion())
                .publishedAt(agent.getPublishedAt())
                .createdAt(agent.getCreatedAt())
                .updatedAt(agent.getUpdatedAt())
                .build();
    }

    private AgentVO toDetailVO(AgentEntity agent, AgentConfigEntity config) {
        AgentVO vo = toSimpleVO(agent);
        if (config != null) {
            vo.setSystemPrompt(resolveSystemPrompt(config));
            vo.setPromptTemplateId(config.getPromptTemplateId());
            vo.setPromptTemplateVersionId(config.getPromptTemplateVersionId());
            vo.setPromptRefMode(config.getPromptRefMode());
            if (config.getPromptTemplateId() != null) {
                vo.setPromptTemplateCurrentVersion(promptTemplateService.getCurrentVersion(
                        config.getTenantId(),
                        config.getPromptTemplateId()
                ));
            }
            vo.setWelcomeMessage(config.getWelcomeMessage());
            vo.setModelConfigId(config.getModelConfigId());
            vo.setTemperature(config.getTemperature());
            vo.setMaxTokens(config.getMaxTokens());
            vo.setMemoryType(config.getMemoryType());
            vo.setMemoryWindow(config.getMemoryWindow());
            vo.setWorkflowId(config.getWorkflowId());
            if (config.getWorkflowId() != null) {
                try {
                    WorkflowEntity workflow = workflowService.requireWorkflow(config.getWorkflowId(), agent.getTenantId());
                    vo.setWorkflowName(workflow.getWorkflowName());
                } catch (BusinessException ignored) {
                    vo.setWorkflowName(null);
                }
            }
            RetrievalConfig retrievalConfig = RetrievalConfigUtils.parse(objectMapper, config.getRetrievalConfig());
            vo.setRetrievalTopK(retrievalConfig.getTopK());
            vo.setRetrievalScoreThreshold(retrievalConfig.getScoreThreshold());
            vo.setRerankEnabled(retrievalConfig.getRerankEnabled());
            vo.setRerankModel(retrievalConfig.getRerankModel());
            vo.setRerankCandidateK(retrievalConfig.getRerankCandidateK());
            vo.setHybridEnabled(retrievalConfig.getHybridEnabled());
            vo.setHybridAlpha(retrievalConfig.getHybridAlpha());
            vo.setTools(resolveAgentTools(agent.getId(), agent.getTenantId(), config.getExtraConfig()));
        } else {
            vo.setTools(resolveAgentTools(agent.getId(), agent.getTenantId(), null));
        }
        vo.setKnowledgeBaseIds(loadKnowledgeBaseIds(agent.getId(), agent.getTenantId()));
        vo.setToolIds(loadToolIds(agent.getId(), agent.getTenantId()));
        vo.setSkillIds(loadSkillIds(agent.getId(), agent.getTenantId()));
        return vo;
    }
}
