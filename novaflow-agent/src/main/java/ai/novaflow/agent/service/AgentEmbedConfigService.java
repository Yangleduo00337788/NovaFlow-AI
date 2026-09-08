package ai.novaflow.agent.service;

import ai.novaflow.agent.domain.AgentEmbedConfig;
import ai.novaflow.agent.domain.dto.AgentEmbedConfigRequest;
import ai.novaflow.agent.domain.vo.AgentEmbedConfigVO;
import ai.novaflow.agent.entity.AgentEntity;
import ai.novaflow.agent.mapper.AgentMapper;
import ai.novaflow.agent.util.AgentEmbedConfigUtils;
import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.user.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AgentEmbedConfigService {

    private final AgentMapper agentMapper;
    private final AgentService agentService;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;

    public AgentEmbedConfigVO getEmbedConfig(Long agentId) {
        AgentEntity agent = agentService.getAgentEntityOrThrow(agentId);
        return toVO(AgentEmbedConfigUtils.parse(objectMapper, agent.getEmbedConfig()));
    }

    @Transactional
    public AgentEmbedConfigVO updateEmbedConfig(Long agentId, AgentEmbedConfigRequest request) {
        agentService.getAgentEntityOrThrow(agentId);
        AgentEmbedConfig config = AgentEmbedConfig.builder()
                .themeColor(request != null ? request.getThemeColor() : null)
                .allowedDomains(request != null ? request.getAllowedDomains() : null)
                .postMessageTargetOrigin(request != null ? request.getPostMessageTargetOrigin() : null)
                .build();
        AgentEmbedConfig normalized = AgentEmbedConfigUtils.normalize(config);
        String json = AgentEmbedConfigUtils.serialize(objectMapper, normalized);
        AgentEntity update = new AgentEntity();
        update.setId(agentId);
        update.setEmbedConfig(json);
        update.setUpdatedAt(LocalDateTime.now());
        int rows = agentMapper.update(update);
        if (rows == 0) {
            throw new BusinessException("Agent不存在");
        }
        auditLogService.record("agent.update_embed_config", "agent", agentId, "更新 Embed 配置");
        return toVO(normalized);
    }

    public AgentEmbedConfig loadConfig(Long agentId) {
        AgentEntity agent = agentMapper.selectOneById(agentId);
        if (agent == null) {
            return new AgentEmbedConfig();
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(agent.getTenantId())) {
            return new AgentEmbedConfig();
        }
        return AgentEmbedConfigUtils.parse(objectMapper, agent.getEmbedConfig());
    }

    public AgentEmbedConfig loadConfig(AgentEntity agent) {
        if (agent == null) {
            return new AgentEmbedConfig();
        }
        return AgentEmbedConfigUtils.parse(objectMapper, agent.getEmbedConfig());
    }

    private AgentEmbedConfigVO toVO(AgentEmbedConfig config) {
        AgentEmbedConfig normalized = AgentEmbedConfigUtils.normalize(config);
        return AgentEmbedConfigVO.builder()
                .themeColor(normalized.getThemeColor())
                .allowedDomains(normalized.getAllowedDomains())
                .postMessageTargetOrigin(normalized.getPostMessageTargetOrigin())
                .build();
    }
}
