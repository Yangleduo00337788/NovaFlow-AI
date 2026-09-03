package ai.novaflow.application.service;

import ai.novaflow.agent.entity.AgentEntity;
import ai.novaflow.agent.mapper.AgentMapper;
import ai.novaflow.application.domain.vo.PortalAppDetailVO;
import ai.novaflow.application.domain.vo.PortalAppVO;
import ai.novaflow.application.entity.ApplicationEntity;
import ai.novaflow.application.mapper.ApplicationMapper;
import ai.novaflow.chat.domain.vo.ConversationMessageVO;
import ai.novaflow.chat.domain.vo.ConversationVO;
import ai.novaflow.chat.service.ConversationService;
import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PortalService {

    private static final int PUBLISH_STATUS_PUBLISHED = 1;

    private final ApplicationMapper applicationMapper;
    private final AgentMapper agentMapper;
    private final ConversationService conversationService;

    public List<PortalAppVO> listPublishedApps() {
        Long tenantId = requireTenantId();
        return applicationMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
                        .eq("status", 1)
                        .eq("publish_status", PUBLISH_STATUS_PUBLISHED)
                        .isNotNull("default_agent_id")
                        .orderBy("published_at", false)
                        .orderBy("app_name", true)
        ).stream().map(this::toPortalVO).toList();
    }

    public PortalAppDetailVO getPublishedApp(Long applicationId) {
        ApplicationEntity entity = getPublishedAppOrThrow(applicationId);
        AgentEntity agent = agentMapper.selectOneById(entity.getDefaultAgentId());
        if (agent == null || agent.getIsDeleted() != 0) {
            throw new BusinessException("应用默认 Agent 不可用");
        }
        return PortalAppDetailVO.builder()
                .applicationId(entity.getId())
                .appName(entity.getAppName())
                .description(entity.getDescription())
                .defaultAgentId(agent.getId())
                .defaultAgentName(agent.getAgentName())
                .portalPath(buildPortalPath(entity.getId()))
                .build();
    }

    public PageResult<ConversationVO> listMyConversations(Long applicationId, int page, int pageSize) {
        ApplicationEntity app = getPublishedAppOrThrow(applicationId);
        return conversationService.pageConversations(
                app.getDefaultAgentId(),
                requireTenantId(),
                null,
                null,
                StpUtil.getLoginIdAsLong(),
                conversationKeyPrefix(applicationId),
                page,
                pageSize);
    }

    public List<ConversationMessageVO> listMyMessages(Long applicationId, String conversationKey) {
        if (!StringUtils.hasText(conversationKey)
                || !conversationKey.startsWith(conversationKeyPrefix(applicationId))) {
            throw new BusinessException("会话不存在");
        }
        ApplicationEntity app = getPublishedAppOrThrow(applicationId);
        return conversationService.listMessages(
                app.getDefaultAgentId(),
                requireTenantId(),
                conversationKey.trim(),
                null,
                StpUtil.getLoginIdAsLong());
    }

    public static String conversationKeyPrefix(Long applicationId) {
        return "portal-" + applicationId + "-";
    }

    public ApplicationEntity getPublishedAppOrThrow(Long applicationId) {
        Long tenantId = requireTenantId();
        ApplicationEntity entity = applicationMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", applicationId)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
                        .eq("status", 1)
                        .eq("publish_status", PUBLISH_STATUS_PUBLISHED)
                        .limit(1)
        );
        if (entity == null || entity.getDefaultAgentId() == null) {
            throw new BusinessException("应用不存在或未发布");
        }
        return entity;
    }

    private PortalAppVO toPortalVO(ApplicationEntity entity) {
        return PortalAppVO.builder()
                .id(entity.getId())
                .appName(entity.getAppName())
                .description(entity.getDescription())
                .icon(entity.getIcon())
                .appType(entity.getAppType())
                .defaultAgentId(entity.getDefaultAgentId())
                .defaultAgentName(resolveAgentName(entity.getDefaultAgentId()))
                .publishedAt(entity.getPublishedAt())
                .portalPath(buildPortalPath(entity.getId()))
                .build();
    }

    private String resolveAgentName(Long agentId) {
        if (agentId == null) {
            return null;
        }
        AgentEntity agent = agentMapper.selectOneById(agentId);
        return agent != null ? agent.getAgentName() : null;
    }

    public static String buildPortalPath(Long applicationId) {
        return "/portal/apps/" + applicationId;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("未获取到租户上下文");
        }
        return tenantId;
    }
}
