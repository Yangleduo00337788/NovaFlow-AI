package ai.novaflow.user.service;

import ai.novaflow.user.entity.UserRecentAccessEntity;
import ai.novaflow.user.mapper.UserRecentAccessMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecentAccessService {

    private final UserRecentAccessMapper userRecentAccessMapper;

    @Transactional
    public void record(Long tenantId, Long userId, String resourceType, Long resourceId, String resourceName) {
        UserRecentAccessEntity existing = findExisting(userId, resourceType, resourceId);
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setTenantId(tenantId);
            existing.setResourceName(resourceName);
            existing.setAccessedAt(now);
            userRecentAccessMapper.update(existing);
            return;
        }
        UserRecentAccessEntity entity = new UserRecentAccessEntity();
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setResourceType(resourceType);
        entity.setResourceId(resourceId);
        entity.setResourceName(resourceName);
        entity.setAccessedAt(now);
        try {
            userRecentAccessMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            UserRecentAccessEntity conflict = findExisting(userId, resourceType, resourceId);
            if (conflict == null) {
                throw ex;
            }
            conflict.setTenantId(tenantId);
            conflict.setResourceName(resourceName);
            conflict.setAccessedAt(now);
            userRecentAccessMapper.update(conflict);
        }
    }

    private UserRecentAccessEntity findExisting(Long userId, String resourceType, Long resourceId) {
        return userRecentAccessMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("user_id", userId)
                        .eq("resource_type", resourceType)
                        .eq("resource_id", resourceId)
                        .limit(1)
        );
    }

    public List<UserRecentAccessEntity> listRecent(Long tenantId, Long userId, int limit) {
        return userRecentAccessMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("user_id", userId)
                        .orderBy("accessed_at", false)
                        .limit(limit)
        );
    }
}
