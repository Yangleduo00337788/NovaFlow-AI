package ai.novaflow.user.service;

import ai.novaflow.user.entity.UserFavoriteEntity;
import ai.novaflow.user.mapper.UserFavoriteMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final UserFavoriteMapper userFavoriteMapper;

    @Transactional
    public boolean toggle(Long tenantId, Long userId, String resourceType, Long resourceId, String resourceName) {
        UserFavoriteEntity existing = findExisting(userId, resourceType, resourceId);
        if (existing != null) {
            userFavoriteMapper.deleteById(existing.getId());
            return false;
        }
        UserFavoriteEntity entity = new UserFavoriteEntity();
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setResourceType(resourceType);
        entity.setResourceId(resourceId);
        entity.setResourceName(resourceName);
        entity.setCreatedAt(LocalDateTime.now());
        try {
            userFavoriteMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            UserFavoriteEntity conflict = findExisting(userId, resourceType, resourceId);
            if (conflict != null) {
                userFavoriteMapper.deleteById(conflict.getId());
                return false;
            }
            throw ex;
        }
        return true;
    }

    public List<UserFavoriteEntity> listFavorites(Long tenantId, Long userId, int limit) {
        return userFavoriteMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("user_id", userId)
                        .orderBy("created_at", false)
                        .limit(limit)
        );
    }

    public Set<String> favoriteKeys(Long userId) {
        List<UserFavoriteEntity> records = userFavoriteMapper.selectListByQuery(
                QueryWrapper.create().eq("user_id", userId)
        );
        Set<String> keys = new HashSet<>();
        for (UserFavoriteEntity record : records) {
            keys.add(key(record.getResourceType(), record.getResourceId()));
        }
        return keys;
    }

    public String key(String resourceType, Long resourceId) {
        return resourceType + ":" + resourceId;
    }

    private UserFavoriteEntity findExisting(Long userId, String resourceType, Long resourceId) {
        return userFavoriteMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("user_id", userId)
                        .eq("resource_type", resourceType)
                        .eq("resource_id", resourceId)
                        .limit(1)
        );
    }
}
