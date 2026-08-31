package ai.novaflow.user.service;

import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.user.domain.vo.UserNotificationVO;
import ai.novaflow.user.entity.RoleEntity;
import ai.novaflow.tenant.entity.TenantMemberEntity;
import ai.novaflow.user.entity.UserNotificationEntity;
import ai.novaflow.user.mapper.RoleMapper;
import ai.novaflow.tenant.mapper.TenantMemberMapper;
import ai.novaflow.user.mapper.UserNotificationMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserNotificationMapper userNotificationMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final RoleMapper roleMapper;

    public PageResult<UserNotificationVO> page(Long tenantId, Long userId, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 50);
        int offset = (safePage - 1) * safePageSize;

        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("user_id", userId)
                .orderBy("created_at", false);

        long total = userNotificationMapper.selectCountByQuery(query);
        List<UserNotificationVO> list = userNotificationMapper.selectListByQuery(
                        query.limit(offset, safePageSize))
                .stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(list, total, safePage, safePageSize);
    }

    public long unreadCount(Long tenantId, Long userId) {
        return userNotificationMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("user_id", userId)
                        .eq("is_read", 0)
        );
    }

    @Transactional
    public void markRead(Long tenantId, Long userId, Long notificationId) {
        UserNotificationEntity entity = userNotificationMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", notificationId)
                        .eq("tenant_id", tenantId)
                        .eq("user_id", userId)
        );
        if (entity == null) {
            throw new BusinessException("通知不存在");
        }
        if (entity.getIsRead() != null && entity.getIsRead() == 1) {
            return;
        }
        entity.setIsRead(1);
        userNotificationMapper.update(entity);
    }

    @Transactional
    public void markAllRead(Long tenantId, Long userId) {
        List<UserNotificationEntity> unread = userNotificationMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("user_id", userId)
                        .eq("is_read", 0)
        );
        for (UserNotificationEntity entity : unread) {
            entity.setIsRead(1);
            userNotificationMapper.update(entity);
        }
    }

    @Transactional
    public void notifyTenantAdmins(
            Long tenantId,
            String category,
            String title,
            String content,
            String linkUrl) {
        if (tenantId == null || !StringUtils.hasText(title)) {
            return;
        }
        Set<Long> adminUserIds = resolveTenantAdminUserIds(tenantId);
        if (adminUserIds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Long userId : adminUserIds) {
            UserNotificationEntity entity = new UserNotificationEntity();
            entity.setTenantId(tenantId);
            entity.setUserId(userId);
            entity.setCategory(category);
            entity.setTitle(title);
            entity.setContent(content);
            entity.setLinkUrl(linkUrl);
            entity.setIsRead(0);
            entity.setCreatedAt(now);
            userNotificationMapper.insert(entity);
        }
    }

    private Set<Long> resolveTenantAdminUserIds(Long tenantId) {
        List<TenantMemberEntity> members = tenantMemberMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("status", 1)
                        .eq("is_deleted", 0)
        );
        if (members.isEmpty()) {
            return Set.of();
        }
        List<Long> roleIds = members.stream()
                .map(TenantMemberEntity::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        Map<Long, RoleEntity> roleMap = roleMapper.selectListByQuery(
                        QueryWrapper.create().in("id", roleIds))
                .stream()
                .collect(Collectors.toMap(RoleEntity::getId, role -> role, (a, b) -> a));

        Set<Long> userIds = members.stream()
                .filter(member -> {
                    RoleEntity role = roleMap.get(member.getRoleId());
                    if (role == null || role.getRoleCode() == null) {
                        return false;
                    }
                    String code = role.getRoleCode();
                    return "tenant_admin".equals(code) || "super_admin".equals(code);
                })
                .map(TenantMemberEntity::getUserId)
                .collect(Collectors.toSet());
        return userIds;
    }

    private UserNotificationVO toVO(UserNotificationEntity entity) {
        return UserNotificationVO.builder()
                .id(entity.getId())
                .category(entity.getCategory())
                .title(entity.getTitle())
                .content(entity.getContent())
                .linkUrl(entity.getLinkUrl())
                .read(entity.getIsRead() != null && entity.getIsRead() == 1)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
