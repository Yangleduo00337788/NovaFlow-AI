package ai.novaflow.application.service;
import ai.novaflow.common.context.TenantContexts;

import ai.novaflow.agent.entity.AgentEntity;
import ai.novaflow.agent.mapper.AgentMapper;
import ai.novaflow.application.domain.vo.PortalAppDetailVO;
import ai.novaflow.application.domain.vo.PortalAppVO;
import ai.novaflow.application.domain.vo.PortalBrandingVO;
import ai.novaflow.application.domain.vo.PortalCategoryVO;
import ai.novaflow.application.entity.ApplicationEntity;
import ai.novaflow.application.mapper.ApplicationMapper;
import ai.novaflow.application.support.PortalCategories;
import ai.novaflow.chat.domain.vo.ConversationMessageVO;
import ai.novaflow.chat.domain.vo.ConversationVO;
import ai.novaflow.chat.service.ConversationService;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.security.PermissionCodes;
import ai.novaflow.common.security.ResourceTypes;
import ai.novaflow.tenant.entity.TenantEntity;
import ai.novaflow.tenant.mapper.TenantMapper;
import ai.novaflow.tenant.service.ResourceAccessService;
import ai.novaflow.user.service.FavoriteService;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortalService {

    public static final String FAVORITE_RESOURCE_TYPE = "application";

    private static final int PUBLISH_STATUS_PUBLISHED = 1;

    private final ApplicationMapper applicationMapper;
    private final AgentMapper agentMapper;
    private final ConversationService conversationService;
    private final ResourceAccessService resourceAccessService;
    private final FavoriteService favoriteService;
    private final TenantMapper tenantMapper;
    private final ObjectMapper objectMapper;

    public PortalBrandingVO getBranding() {
        Long tenantId = TenantContexts.requireTenantId();
        TenantEntity tenant = tenantMapper.selectOneById(tenantId);
        if (tenant == null || tenant.getIsDeleted() != 0) {
            throw new BusinessException("企业不存在");
        }
        return PortalBrandingVO.builder()
                .tenantName(tenant.getTenantName())
                .logoUrl(tenant.getLogoUrl())
                .portalThemeColor(normalizeThemeColor(tenant.getPortalThemeColor()))
                .portalSubtitle("你的 AI 办公助手")
                .build();
    }

    public List<PortalCategoryVO> listCategories() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (PortalAppVO app : listPublishedApps(null, false)) {
            String code = PortalCategories.normalize(app.getPortalCategory());
            counts.merge(code, 1, Integer::sum);
        }
        List<PortalCategoryVO> categories = new ArrayList<>();
        for (Map.Entry<String, String> entry : PortalCategories.predefined().entrySet()) {
            int count = counts.getOrDefault(entry.getKey(), 0);
            if (count > 0) {
                categories.add(PortalCategoryVO.builder()
                        .code(entry.getKey())
                        .label(entry.getValue())
                        .appCount(count)
                        .build());
            }
        }
        counts.forEach((code, count) -> {
            if (!PortalCategories.predefined().containsKey(code)) {
                categories.add(PortalCategoryVO.builder()
                        .code(code)
                        .label(PortalCategories.labelOf(code))
                        .appCount(count)
                        .build());
            }
        });
        return categories;
    }

    public List<PortalAppVO> listPublishedApps(String category, boolean favoritesOnly) {
        Long tenantId = TenantContexts.requireTenantId();
        long userId = StpUtil.getLoginIdAsLong();
        Set<String> favoriteKeys = favoriteService.favoriteKeys(userId);
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", 0)
                .eq("status", 1)
                .eq("publish_status", PUBLISH_STATUS_PUBLISHED)
                .isNotNull("default_agent_id")
                .orderBy("published_at", false)
                .orderBy("app_name", true);
        resourceAccessService.applyReadableFilterAny(
                query,
                userId,
                tenantId,
                ResourceTypes.APPLICATION,
                List.of(PermissionCodes.APPLICATION_READ, PermissionCodes.PORTAL_ACCESS),
                "application.id");
        String normalizedCategory = StringUtils.hasText(category) ? PortalCategories.normalize(category) : null;
        return applicationMapper.selectListByQuery(query).stream()
                .map(entity -> toPortalVO(entity, favoriteKeys))
                .filter(app -> normalizedCategory == null
                        || PortalCategories.normalize(app.getPortalCategory()).equals(normalizedCategory))
                .filter(app -> !favoritesOnly || Boolean.TRUE.equals(app.getFavorited()))
                .sorted(Comparator
                        .comparing((PortalAppVO app) -> !Boolean.TRUE.equals(app.getFavorited()))
                        .thenComparing(PortalAppVO::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PortalAppVO::getAppName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public boolean toggleFavorite(Long applicationId) {
        ApplicationEntity entity = getPublishedAppOrThrow(applicationId);
        return favoriteService.toggle(
                TenantContexts.requireTenantId(),
                StpUtil.getLoginIdAsLong(),
                FAVORITE_RESOURCE_TYPE,
                entity.getId(),
                entity.getAppName());
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
                TenantContexts.requireTenantId(),
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
                TenantContexts.requireTenantId(),
                conversationKey.trim(),
                null,
                StpUtil.getLoginIdAsLong());
    }

    public String exportConversation(Long applicationId, String conversationKey, String format) {
        ApplicationEntity app = getPublishedAppOrThrow(applicationId);
        List<ConversationMessageVO> messages = listMyMessages(applicationId, conversationKey);
        if (messages.isEmpty()) {
            throw new BusinessException("会话暂无消息可导出");
        }
        String normalized = StringUtils.hasText(format) ? format.trim().toLowerCase(Locale.ROOT) : "markdown";
        if ("json".equals(normalized)) {
            try {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(messages);
            } catch (JsonProcessingException ex) {
                throw new BusinessException("导出失败");
            }
        }
        StringBuilder markdown = new StringBuilder();
        markdown.append("# ").append(app.getAppName()).append(" 对话导出\n\n");
        markdown.append("> conversationKey: ").append(conversationKey.trim()).append("\n\n");
        for (ConversationMessageVO message : messages) {
            String roleLabel = "assistant".equalsIgnoreCase(message.getRole()) ? "助手" : "用户";
            markdown.append("## ").append(roleLabel);
            if (message.getCreatedAt() != null) {
                markdown.append(" (").append(message.getCreatedAt().toString().replace('T', ' ')).append(')');
            }
            markdown.append("\n\n").append(message.getContent() == null ? "" : message.getContent().trim())
                    .append("\n\n");
        }
        return markdown.toString();
    }

    public static String conversationKeyPrefix(Long applicationId) {
        return "portal-" + applicationId + "-";
    }

    public ApplicationEntity getPublishedAppOrThrow(Long applicationId) {
        Long tenantId = TenantContexts.requireTenantId();
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
        resourceAccessService.requireResourceAccessAny(
                StpUtil.getLoginIdAsLong(),
                tenantId,
                ResourceTypes.APPLICATION,
                applicationId,
                PermissionCodes.APPLICATION_READ,
                PermissionCodes.PORTAL_ACCESS);
        return entity;
    }

    private PortalAppVO toPortalVO(ApplicationEntity entity, Set<String> favoriteKeys) {
        boolean favorited = favoriteKeys.contains(
                favoriteService.key(FAVORITE_RESOURCE_TYPE, entity.getId()));
        return PortalAppVO.builder()
                .id(entity.getId())
                .appName(entity.getAppName())
                .description(entity.getDescription())
                .icon(entity.getIcon())
                .portalCategory(PortalCategories.normalize(entity.getPortalCategory()))
                .appType(entity.getAppType())
                .defaultAgentId(entity.getDefaultAgentId())
                .defaultAgentName(resolveAgentName(entity.getDefaultAgentId()))
                .publishedAt(entity.getPublishedAt())
                .portalPath(buildPortalPath(entity.getId()))
                .favorited(favorited)
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

    static String normalizeThemeColor(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.matches("^#[0-9A-Fa-f]{6}$")) {
            return trimmed.toLowerCase(Locale.ROOT);
        }
        return null;
    }
}
