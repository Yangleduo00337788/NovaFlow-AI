package ai.novaflow.user.service;

import ai.novaflow.common.audit.AuditRecorder;
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
public class AuditLogService implements AuditRecorder {

    private final AuditLogMapper auditLogMapper;

    @Override
    public void record(String action, String resourceType, Long resourceId, String detail) {
        insert(action, resourceType, resourceId, detail, TenantContext.getTenantId(),
                StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null, resolveClientIp());
    }

    @Override
    public void record(String action, String resourceType, Long resourceId, String detail, Long tenantId, Long userId) {
        insert(action, resourceType, resourceId, detail, tenantId, userId, resolveClientIp());
    }

    @Override
    public void record(String action, String resourceType, Long resourceId, String detail,
                       Long tenantId, Long userId, String clientIp) {
        insert(action, resourceType, resourceId, detail, tenantId, userId, clientIp);
    }

    private void insert(String action, String resourceType, Long resourceId, String detail,
                        Long tenantId, Long userId, String clientIp) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setAction(action);
        entity.setResourceType(resourceType);
        entity.setResourceId(resourceId);
        entity.setDetail(truncate(detail));
        entity.setClientIp(clientIp);
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
