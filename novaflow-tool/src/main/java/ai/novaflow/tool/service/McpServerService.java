package ai.novaflow.tool.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.tool.domain.dto.McpServerSaveRequest;
import ai.novaflow.tool.domain.vo.McpServerVO;
import ai.novaflow.tool.entity.McpServerEntity;
import ai.novaflow.tool.mapper.McpServerMapper;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class McpServerService {

    private final McpServerMapper mcpServerMapper;
    private final ObjectMapper objectMapper;

    public PageResult<McpServerVO> page(int page, int pageSize, String keyword) {
        Long tenantId = requireTenantId();
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", 0);
        if (StringUtils.hasText(keyword)) {
            query.and("(server_name like ? or description like ?)",
                    "%" + keyword.trim() + "%", "%" + keyword.trim() + "%");
        }
        query.orderBy("updated_at", false);
        Page<McpServerEntity> result = mcpServerMapper.paginate(Page.of(page, pageSize), query);
        List<McpServerVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getTotalRow(), page, pageSize);
    }

    @Transactional
    public McpServerVO create(McpServerSaveRequest request) {
        Long tenantId = requireTenantId();
        ensureNameUnique(tenantId, request.getServerName(), null);
        LocalDateTime now = LocalDateTime.now();
        McpServerEntity entity = new McpServerEntity();
        entity.setTenantId(tenantId);
        entity.setServerName(request.getServerName().trim());
        entity.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        entity.setTransportType(request.getTransportType().trim());
        entity.setServerConfig(buildConfig(request));
        entity.setStatus(0);
        entity.setCreatedBy(StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setIsDeleted(0);
        mcpServerMapper.insert(entity);
        return toVO(entity);
    }

    @Transactional
    public void delete(Long id) {
        McpServerEntity entity = getOrThrow(id);
        entity.setIsDeleted(1);
        entity.setUpdatedAt(LocalDateTime.now());
        mcpServerMapper.update(entity);
    }

    private McpServerEntity getOrThrow(Long id) {
        Long tenantId = requireTenantId();
        McpServerEntity entity = mcpServerMapper.selectOneById(id);
        if (entity == null || entity.getIsDeleted() != null && entity.getIsDeleted() != 0
                || !tenantId.equals(entity.getTenantId())) {
            throw new BusinessException("MCP 服务不存在");
        }
        return entity;
    }

    private void ensureNameUnique(Long tenantId, String serverName, Long excludeId) {
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("server_name", serverName.trim())
                .eq("is_deleted", 0);
        if (excludeId != null) {
            query.ne("id", excludeId);
        }
        if (mcpServerMapper.selectCountByQuery(query) > 0) {
            throw new BusinessException("MCP 服务名称已存在");
        }
    }

    private String buildConfig(McpServerSaveRequest request) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "transportType", request.getTransportType().trim(),
                    "endpoint", request.getEndpoint().trim()
            ));
        } catch (Exception e) {
            throw new BusinessException("MCP 服务配置无效");
        }
    }

    private McpServerVO toVO(McpServerEntity entity) {
        String endpoint = extractEndpoint(entity.getServerConfig());
        int toolCount = 0;
        if (StringUtils.hasText(entity.getDiscoveredTools())) {
            try {
                Object parsed = objectMapper.readValue(entity.getDiscoveredTools(), Object.class);
                if (parsed instanceof List<?> list) {
                    toolCount = list.size();
                }
            } catch (Exception ignored) {
                toolCount = 0;
            }
        }
        int status = entity.getStatus() != null ? entity.getStatus() : 0;
        return McpServerVO.builder()
                .id(entity.getId())
                .serverName(entity.getServerName())
                .description(entity.getDescription())
                .transportType(entity.getTransportType())
                .endpoint(endpoint)
                .status(status)
                .statusLabel(statusLabel(status))
                .toolCount(toolCount)
                .lastConnectedAt(entity.getLastConnectedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String extractEndpoint(String serverConfig) {
        if (!StringUtils.hasText(serverConfig)) {
            return "";
        }
        try {
            Map<?, ?> config = objectMapper.readValue(serverConfig, Map.class);
            Object endpoint = config.get("endpoint");
            return endpoint != null ? String.valueOf(endpoint) : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String statusLabel(int status) {
        return switch (status) {
            case 1 -> "已连接";
            case 2 -> "错误";
            default -> "未连接";
        };
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户上下文缺失");
        }
        return tenantId;
    }
}
