package ai.novaflow.user.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.user.entity.AuditLogEntity;
import ai.novaflow.user.mapper.AuditLogMapper;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    public void record(String action, String resourceType, Long resourceId, String detail) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setTenantId(TenantContext.getTenantId());
        entity.setUserId(StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null);
        entity.setAction(action);
        entity.setResourceType(resourceType);
        entity.setResourceId(resourceId);
        entity.setDetail(truncate(detail));
        entity.setClientIp(resolveClientIp());
        entity.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(entity);
    }

    public void record(String action, String resourceType, Long resourceId, String detail, Long tenantId, Long userId) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setAction(action);
        entity.setResourceType(resourceType);
        entity.setResourceId(resourceId);
        entity.setDetail(truncate(detail));
        entity.setClientIp(resolveClientIp());
        entity.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(entity);
    }

    private String truncate(String detail) {
        if (!StringUtils.hasText(detail)) {
            return null;
        }
        String trimmed = detail.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }

    private String resolveClientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return request != null ? request.getRemoteAddr() : null;
    }
}
