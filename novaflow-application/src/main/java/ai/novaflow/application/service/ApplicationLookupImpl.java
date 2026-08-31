package ai.novaflow.application.service;

import ai.novaflow.application.entity.ApplicationEntity;
import ai.novaflow.application.mapper.ApplicationMapper;
import ai.novaflow.common.application.ApplicationLookup;
import ai.novaflow.common.exception.BusinessException;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationLookupImpl implements ApplicationLookup {

    private final ApplicationMapper applicationMapper;

    @Override
    public void requireExists(Long tenantId, Long applicationId) {
        if (applicationId == null) {
            throw new BusinessException("所属应用不能为空");
        }
        ApplicationEntity application = applicationMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", applicationId)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
        );
        if (application == null) {
            throw new BusinessException("应用不存在");
        }
    }

    @Override
    public Map<Long, String> getApplicationNameMap(List<Long> applicationIds) {
        List<Long> ids = distinctIds(applicationIds);
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> map = applicationMapper.selectListByQuery(QueryWrapper.create().in("id", ids))
                .stream()
                .collect(Collectors.toMap(ApplicationEntity::getId, ApplicationEntity::getAppName, (a, b) -> a));
        for (Long applicationId : ids) {
            map.putIfAbsent(applicationId, "未知应用");
        }
        return map;
    }

    private List<Long> distinctIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toCollection(ArrayList::new));
    }
}
