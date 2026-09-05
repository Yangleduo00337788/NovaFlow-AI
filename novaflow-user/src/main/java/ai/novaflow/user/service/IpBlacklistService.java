package ai.novaflow.user.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.security.IpBlacklistChecker;
import ai.novaflow.common.util.PageQueryUtils;
import ai.novaflow.user.domain.dto.IpBlacklistCreateRequest;
import ai.novaflow.user.domain.dto.IpBlacklistUpdateRequest;
import ai.novaflow.user.domain.vo.IpBlacklistVO;
import ai.novaflow.user.entity.IpBlacklistEntity;
import ai.novaflow.user.mapper.IpBlacklistMapper;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class IpBlacklistService implements IpBlacklistChecker {

    private static final int BLOCKED_CODE = 40301;

    private final IpBlacklistMapper ipBlacklistMapper;
    private final PermissionService permissionService;
    private final AuditLogService auditLogService;

    private final AtomicReference<Set<String>> blockedIps = new AtomicReference<>(Set.of());

    @PostConstruct
    void initCache() {
        refreshCache();
    }

    @Override
    public void requireAllowed(String clientIp) {
        if (isBlocked(clientIp)) {
            throw new BusinessException(BLOCKED_CODE, "IP 已被禁止访问");
        }
    }

    @Override
    public boolean isBlocked(String clientIp) {
        String normalized = normalizeIp(clientIp);
        if (!StringUtils.hasText(normalized)) {
            return false;
        }
        return blockedIps.get().contains(normalized);
    }

    public PageResult<IpBlacklistVO> page(int page, int pageSize, String keyword) {
        requireSuperAdmin();
        page = PageQueryUtils.normalizePage(page);
        pageSize = PageQueryUtils.normalizePageSize(pageSize);

        QueryWrapper query = QueryWrapper.create().eq("is_deleted", 0);
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword.trim() + "%";
            query.and("(ip_address LIKE ? OR reason LIKE ?)", like, like);
        }
        query.orderBy("created_at", false);

        Page<IpBlacklistEntity> result = ipBlacklistMapper.paginate(Page.of(page, pageSize), query);
        List<IpBlacklistVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getTotalRow(), page, pageSize);
    }

    @Transactional
    public IpBlacklistVO create(IpBlacklistCreateRequest request) {
        requireSuperAdmin();
        String ipAddress = validateAndNormalizeIp(request.getIpAddress());
        ensureNoActiveDuplicate(ipAddress, null);

        IpBlacklistEntity entity = new IpBlacklistEntity();
        entity.setIpAddress(ipAddress);
        entity.setReason(trimToNull(request.getReason()));
        entity.setStatus(1);
        entity.setExpireAt(request.getExpireAt());
        entity.setCreatedBy(StpUtil.getLoginIdAsLong());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setIsDeleted(0);
        ipBlacklistMapper.insert(entity);
        refreshCache();

        auditLogService.record(
                "platform.ip_blacklist.create",
                "ip_blacklist",
                entity.getId(),
                "封禁 IP: " + ipAddress,
                TenantContext.getTenantId(),
                StpUtil.getLoginIdAsLong());

        return toVO(entity);
    }

    @Transactional
    public IpBlacklistVO update(Long id, IpBlacklistUpdateRequest request) {
        requireSuperAdmin();
        IpBlacklistEntity entity = getEntityOrThrow(id);
        entity.setReason(trimToNull(request.getReason()));
        entity.setStatus(request.getStatus());
        entity.setExpireAt(request.getExpireAt());
        entity.setUpdatedAt(LocalDateTime.now());
        ipBlacklistMapper.update(entity);
        refreshCache();

        String action = request.getStatus() == 1 ? "platform.ip_blacklist.enable" : "platform.ip_blacklist.disable";
        auditLogService.record(
                action,
                "ip_blacklist",
                entity.getId(),
                (request.getStatus() == 1 ? "启用" : "停用") + " IP 黑名单: " + entity.getIpAddress(),
                TenantContext.getTenantId(),
                StpUtil.getLoginIdAsLong());

        return toVO(entity);
    }

    @Transactional
    public void delete(Long id) {
        requireSuperAdmin();
        IpBlacklistEntity entity = getEntityOrThrow(id);
        entity.setIsDeleted(1);
        entity.setStatus(0);
        entity.setUpdatedAt(LocalDateTime.now());
        ipBlacklistMapper.update(entity);
        refreshCache();

        auditLogService.record(
                "platform.ip_blacklist.delete",
                "ip_blacklist",
                entity.getId(),
                "删除 IP 黑名单: " + entity.getIpAddress(),
                TenantContext.getTenantId(),
                StpUtil.getLoginIdAsLong());
    }

    void refreshCache() {
        LocalDateTime now = LocalDateTime.now();
        List<IpBlacklistEntity> rows = ipBlacklistMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("is_deleted", 0)
                        .eq("status", 1));
        Set<String> active = new HashSet<>();
        for (IpBlacklistEntity row : rows) {
            if (!StringUtils.hasText(row.getIpAddress())) {
                continue;
            }
            if (row.getExpireAt() != null && !row.getExpireAt().isAfter(now)) {
                continue;
            }
            active.add(row.getIpAddress());
        }
        blockedIps.set(Set.copyOf(active));
    }

    private void ensureNoActiveDuplicate(String ipAddress, Long excludeId) {
        QueryWrapper query = QueryWrapper.create()
                .eq("is_deleted", 0)
                .eq("status", 1)
                .eq("ip_address", ipAddress);
        if (excludeId != null) {
            query.ne("id", excludeId);
        }
        if (ipBlacklistMapper.selectCountByQuery(query) > 0) {
            throw new BusinessException("该 IP 已在黑名单中");
        }
    }

    private IpBlacklistEntity getEntityOrThrow(Long id) {
        IpBlacklistEntity entity = ipBlacklistMapper.selectOneByQuery(
                QueryWrapper.create().eq("id", id).eq("is_deleted", 0));
        if (entity == null) {
            throw new BusinessException("IP 黑名单记录不存在");
        }
        return entity;
    }

    private String validateAndNormalizeIp(String ipAddress) {
        String normalized = normalizeIp(ipAddress);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("IP 地址无效");
        }
        try {
            InetAddress.getByName(normalized);
        } catch (UnknownHostException ex) {
            throw new BusinessException("IP 地址无效");
        }
        return normalized;
    }

    private String normalizeIp(String ipAddress) {
        if (!StringUtils.hasText(ipAddress)) {
            return null;
        }
        return ipAddress.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void requireSuperAdmin() {
        permissionService.requireSuperAdmin(StpUtil.getLoginIdAsLong(), TenantContext.getTenantId());
    }

    private IpBlacklistVO toVO(IpBlacklistEntity entity) {
        return IpBlacklistVO.builder()
                .id(entity.getId())
                .ipAddress(entity.getIpAddress())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .expireAt(entity.getExpireAt())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
