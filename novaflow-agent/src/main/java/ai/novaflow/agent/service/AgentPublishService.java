package ai.novaflow.agent.service;
import ai.novaflow.common.context.TenantContexts;

import ai.novaflow.agent.domain.AgentStatus;
import ai.novaflow.agent.domain.vo.AgentPublishVO;
import ai.novaflow.agent.domain.vo.AgentVO;
import ai.novaflow.agent.entity.AgentApiKeyEntity;
import ai.novaflow.agent.entity.AgentEmbedTokenEntity;
import ai.novaflow.agent.entity.AgentEntity;
import ai.novaflow.agent.mapper.AgentMapper;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.security.PermissionCodes;
import cn.dev33.satoken.stp.StpUtil;
import ai.novaflow.workflow.service.WorkflowService;
import com.mybatisflex.core.query.QueryWrapper;
import ai.novaflow.user.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AgentPublishService {

    private final AgentMapper agentMapper;
    private final AgentService agentService;
    private final AgentApiKeyService agentApiKeyService;
    private final AgentEmbedTokenService agentEmbedTokenService;
    private final WorkflowService workflowService;
    private final AuditLogService auditLogService;

    public AgentPublishVO getPublishInfo(Long agentId) {
        AgentEntity agent = agentService.getAgentEntityOrThrow(agentId);
        AgentApiKeyEntity apiKey = agentApiKeyService.findByAgentId(agentId);
        AgentEmbedTokenEntity embedToken = agentEmbedTokenService.findByAgentId(agentId);
        return redactApiSecretsIfNeeded(buildPublishVO(agent, apiKey, embedToken, null, null));
    }

    @Transactional
    public AgentPublishVO publish(Long agentId) {
        Long tenantId = TenantContexts.requireTenantId();
        AgentEntity agent = lockAgentForUpdate(agentId, tenantId);
        AgentVO detail = agentService.detail(agentId);
        validatePublishable(detail);

        String rawApiKey = agentApiKeyService.issueApiKey(agentId, tenantId);
        String rawEmbedToken = agentEmbedTokenService.issueEmbedToken(agentId, tenantId);
        agent.setStatus(AgentStatus.PUBLISHED);
        agent.setPublishedAt(LocalDateTime.now());
        agent.setVersion(safeVersion(agent.getVersion()) + 1);
        agent.setUpdatedAt(LocalDateTime.now());
        agentMapper.update(agent);

        auditLogService.record("agent.publish", "agent", agentId, "发布 Agent v" + agent.getVersion());
        return redactApiSecretsIfNeeded(buildPublishVO(
                agent,
                agentApiKeyService.findByAgentId(agentId),
                agentEmbedTokenService.findByAgentId(agentId),
                rawApiKey,
                rawEmbedToken));
    }

    @Transactional
    public AgentPublishVO unpublish(Long agentId) {
        AgentEntity agent = agentService.getAgentEntityOrThrow(agentId);
        agent.setStatus(AgentStatus.OFFLINE);
        agent.setUpdatedAt(LocalDateTime.now());
        agentMapper.update(agent);
        agentApiKeyService.disableApiKey(agentId);
        agentEmbedTokenService.disableEmbedToken(agentId);
        auditLogService.record("agent.unpublish", "agent", agentId, "下线 Agent");
        return buildPublishVO(
                agent,
                agentApiKeyService.findByAgentId(agentId),
                agentEmbedTokenService.findByAgentId(agentId),
                null,
                null);
    }

    @Transactional
    public AgentPublishVO rotateApiKey(Long agentId) {
        AgentEntity agent = agentService.getAgentEntityOrThrow(agentId);
        if (agent.getStatus() != AgentStatus.PUBLISHED) {
            throw new BusinessException("仅已发布的 Agent 可轮换 API Key");
        }
        String rawApiKey = agentApiKeyService.issueApiKey(agentId, agent.getTenantId());
        auditLogService.record("agent.rotate_api_key", "agent", agentId, "轮换 API Key");
        return redactApiSecretsIfNeeded(buildPublishVO(
                agent,
                agentApiKeyService.findByAgentId(agentId),
                agentEmbedTokenService.findByAgentId(agentId),
                rawApiKey,
                null));
    }

    @Transactional
    public AgentPublishVO rotateEmbedToken(Long agentId) {
        AgentEntity agent = agentService.getAgentEntityOrThrow(agentId);
        if (agent.getStatus() != AgentStatus.PUBLISHED) {
            throw new BusinessException("仅已发布的 Agent 可轮换 Embed Token");
        }
        String rawEmbedToken = agentEmbedTokenService.issueEmbedToken(agentId, agent.getTenantId());
        auditLogService.record("agent.rotate_embed_token", "agent", agentId, "轮换 Embed Token");
        return redactApiSecretsIfNeeded(buildPublishVO(
                agent,
                agentApiKeyService.findByAgentId(agentId),
                agentEmbedTokenService.findByAgentId(agentId),
                null,
                rawEmbedToken));
    }

    public AgentEntity requirePublishedAgent(Long agentId, Long tenantId) {
        AgentEntity agent = agentMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", agentId)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
        );
        if (agent == null) {
            throw new BusinessException(40401, "Agent 不存在");
        }
        if (agent.getStatus() != AgentStatus.PUBLISHED) {
            throw new BusinessException(40302, "Agent 未发布或已下线");
        }
        return agent;
    }

    private void validatePublishable(AgentVO agent) {
        if ("tool".equals(agent.getAgentType())) {
            if (agent.getTools() == null || agent.getTools().stream()
                    .noneMatch(tool -> tool.getName() != null && !tool.getName().isBlank()
                            && tool.getUrl() != null && !tool.getUrl().isBlank())) {
                throw new BusinessException("Tool Agent 发布前需配置至少一个有效的 HTTP 工具");
            }
            return;
        }
        if ("workflow".equals(agent.getAgentType())) {
            if (agent.getWorkflowId() == null) {
                throw new BusinessException("Workflow Agent 发布前需绑定工作流");
            }
            workflowService.requirePublishedWorkflow(agent.getWorkflowId(), TenantContexts.requireTenantId());
            return;
        }
        if (!"chat".equals(agent.getAgentType()) && !"rag".equals(agent.getAgentType())) {
            throw new BusinessException("当前仅支持发布 Chat / RAG / Tool / Workflow Agent");
        }
        if ("rag".equals(agent.getAgentType())
                && (agent.getKnowledgeBaseIds() == null || agent.getKnowledgeBaseIds().isEmpty())) {
            throw new BusinessException("RAG Agent 发布前需关联知识库");
        }
    }

    private AgentPublishVO buildPublishVO(
            AgentEntity agent,
            AgentApiKeyEntity apiKey,
            AgentEmbedTokenEntity embedToken,
            String rawApiKey,
            String rawEmbedToken) {
        return AgentPublishVO.builder()
                .agentId(agent.getId())
                .status(agent.getStatus())
                .version(agent.getVersion())
                .publishedAt(agent.getPublishedAt())
                .apiKeyPrefix(apiKey != null ? apiKey.getApiKeyPrefix() : null)
                .apiKey(rawApiKey)
                .embedTokenPrefix(embedToken != null ? embedToken.getTokenPrefix() : null)
                .embedToken(rawEmbedToken)
                .chatEndpoint("/api/v1/open/agents/" + agent.getId() + "/chat")
                .streamEndpoint("/api/v1/open/agents/" + agent.getId() + "/chat/stream")
                .welcomeEndpoint("/api/v1/open/agents/" + agent.getId() + "/welcome")
                .embedPath("/embed/agents/" + agent.getId())
                .build();
    }

    private AgentPublishVO redactApiSecretsIfNeeded(AgentPublishVO vo) {
        if (vo == null || canViewApiSecrets()) {
            return vo;
        }
        return AgentPublishVO.builder()
                .agentId(vo.getAgentId())
                .status(vo.getStatus())
                .version(vo.getVersion())
                .publishedAt(vo.getPublishedAt())
                .build();
    }

    private boolean canViewApiSecrets() {
        return StpUtil.hasPermission(PermissionCodes.API_READ)
                || StpUtil.hasPermission(PermissionCodes.API_CREATE)
                || StpUtil.hasPermission(PermissionCodes.API_UPDATE);
    }

    private int safeVersion(Integer version) {
        return version != null ? version : 0;
    }


    private AgentEntity lockAgentForUpdate(Long agentId, Long tenantId) {
        AgentEntity agent = agentMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", agentId)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
                        .forUpdate()
        );
        if (agent == null) {
            throw new BusinessException("Agent不存在");
        }
        return agent;
    }
}
