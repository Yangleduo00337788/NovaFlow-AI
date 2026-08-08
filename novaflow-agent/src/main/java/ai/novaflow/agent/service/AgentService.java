package ai.novaflow.agent.service;

import ai.novaflow.agent.domain.dto.AgentSaveRequest;
import ai.novaflow.agent.domain.vo.AgentVO;
import ai.novaflow.agent.entity.AgentConfigEntity;
import ai.novaflow.agent.entity.AgentEntity;
import ai.novaflow.agent.entity.AgentKnowledgeEntity;
import ai.novaflow.agent.entity.AgentToolEntity;
import ai.novaflow.agent.mapper.AgentConfigMapper;
import ai.novaflow.agent.mapper.AgentKnowledgeMapper;
import ai.novaflow.agent.mapper.AgentMapper;
import ai.novaflow.agent.mapper.AgentToolMapper;
import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.domain.RetrievalConfig;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.agent.util.AgentExtraConfigUtils;
import ai.novaflow.common.util.RetrievalConfigUtils;
import ai.novaflow.tool.domain.HttpToolDefinition;
import ai.novaflow.tool.service.ToolDefinitionService;
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
    private final ToolDefinitionService toolDefinitionService;
    private final RecentAccessService recentAccessService;
    private final ObjectMapper objectMapper;

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
        Long applicationId = request.getApplicationId() != null ? request.getApplicationId() : 1L;

        AgentEntity agent = new AgentEntity();
        agent.setTenantId(tenantId);
        agent.setApplicationId(applicationId);
        agent.setAgentName(request.getAgentName());
        agent.setDescription(request.getDescription());
        agent.setIcon(request.getIcon());
        agent.setAgentType(request.getAgentType());
        agent.setStatus(0);
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

        return toDetailVO(agent, config);
    }

    @Transactional
    public AgentVO update(Long id, AgentSaveRequest request) {
        AgentEntity agent = getAgentOrThrow(id);
        agent.setAgentName(request.getAgentName());
        agent.setDescription(request.getDescription());
        agent.setIcon(request.getIcon());
        agent.setAgentType(request.getAgentType());
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
        return toDetailVO(agent, config);
    }

    @Transactional
    public void delete(Long id) {
        AgentEntity agent = getAgentOrThrow(id);
        agent.setIsDeleted(1);
        agent.setUpdatedAt(LocalDateTime.now());
        agentMapper.update(agent);
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
            toolDefinitionService.getToolOrThrow(toolId);
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
        config.setSystemPrompt(StringUtils.hasText(request.getSystemPrompt())
                ? request.getSystemPrompt().trim()
                : null);
        config.setWelcomeMessage(request.getWelcomeMessage());
        config.setModelConfigId(request.getModelConfigId());
        config.setTemperature(request.getTemperature());
        config.setMaxTokens(request.getMaxTokens());
        config.setMemoryType(request.getMemoryType());
        config.setMemoryWindow(request.getMemoryWindow());
        config.setRetrievalConfig(RetrievalConfigUtils.serialize(objectMapper, toRetrievalConfig(request)));
        config.setExtraConfig(AgentExtraConfigUtils.serializeTools(objectMapper, request.getTools()));
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
            vo.setSystemPrompt(config.getSystemPrompt());
            vo.setWelcomeMessage(config.getWelcomeMessage());
            vo.setModelConfigId(config.getModelConfigId());
            vo.setTemperature(config.getTemperature());
            vo.setMaxTokens(config.getMaxTokens());
            vo.setMemoryType(config.getMemoryType());
            vo.setMemoryWindow(config.getMemoryWindow());
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
        return vo;
    }
}
