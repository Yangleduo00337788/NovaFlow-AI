package ai.novaflow.tool.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.tool.domain.dto.McpServerSaveRequest;
import ai.novaflow.tool.domain.vo.McpConnectResultVO;
import ai.novaflow.tool.domain.vo.McpDiscoveredToolVO;
import ai.novaflow.tool.domain.vo.McpServerVO;
import ai.novaflow.tool.entity.McpServerEntity;
import ai.novaflow.tool.mapper.McpServerMapper;
import ai.novaflow.tool.mcp.McpClient;
import ai.novaflow.tool.mcp.McpConnectResult;
import ai.novaflow.tool.mcp.McpDiscoveredTool;
import ai.novaflow.tool.mcp.McpServerConfig;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class McpServerService {

    private final McpServerMapper mcpServerMapper;
    private final McpClient mcpClient;
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

    public McpServerVO detail(Long id) {
        McpServerEntity entity = getOrThrow(id);
        McpServerVO vo = toVO(entity);
        vo.setTools(parseDiscoveredTools(entity.getDiscoveredTools()));
        return vo;
    }

    @Transactional
    public McpConnectResultVO connect(Long id) {
        McpServerEntity entity = getOrThrow(id);
        McpServerConfig config = parseStoredConfig(entity.getServerName(), entity.getServerConfig());
        McpConnectResult result = mcpClient.discoverTools(config);
        LocalDateTime now = LocalDateTime.now();
        entity.setUpdatedAt(now);
        entity.setLastConnectedAt(now);
        if (result.isSuccess()) {
            entity.setStatus(1);
            try {
                entity.setDiscoveredTools(objectMapper.writeValueAsString(result.getTools()));
            } catch (Exception e) {
                throw new BusinessException("保存 MCP 工具列表失败");
            }
        } else {
            entity.setStatus(2);
        }
        mcpServerMapper.update(entity);
        return toConnectResultVO(entity, result);
    }

    @Transactional
    public McpServerVO create(McpServerSaveRequest request) {
        Long tenantId = requireTenantId();
        ensureNameUnique(tenantId, request.getServerName(), null);
        McpServerConfig config = McpServerConfig.parse(objectMapper, request.getServerName(), request.getServerConfig());
        LocalDateTime now = LocalDateTime.now();
        McpServerEntity entity = new McpServerEntity();
        entity.setTenantId(tenantId);
        entity.setServerName(request.getServerName().trim());
        entity.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        entity.setTransportType(config.getTransportType());
        entity.setServerConfig(config.toStorageJson(objectMapper));
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

    private McpServerConfig parseStoredConfig(String serverName, String serverConfig) {
        return McpServerConfig.parse(objectMapper, serverName, serverConfig);
    }

    private McpConnectResultVO toConnectResultVO(McpServerEntity entity, McpConnectResult result) {
        List<McpDiscoveredToolVO> tools = result.getTools() != null
                ? result.getTools().stream().map(this::toToolVO).toList()
                : List.of();
        int status = entity.getStatus() != null ? entity.getStatus() : 0;
        return McpConnectResultVO.builder()
                .id(entity.getId())
                .serverName(entity.getServerName())
                .status(status)
                .statusLabel(statusLabel(status))
                .toolCount(tools.size())
                .message(result.getMessage())
                .lastConnectedAt(entity.getLastConnectedAt())
                .tools(tools)
                .build();
    }

    private McpDiscoveredToolVO toToolVO(McpDiscoveredTool tool) {
        return McpDiscoveredToolVO.builder()
                .name(tool.getName())
                .description(tool.getDescription())
                .inputSchema(tool.getInputSchema())
                .build();
    }

    private List<McpDiscoveredToolVO> parseDiscoveredTools(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) {
                return List.of();
            }
            List<McpDiscoveredToolVO> tools = new ArrayList<>();
            for (JsonNode node : root) {
                String name = node.path("name").asText("");
                if (!StringUtils.hasText(name)) {
                    continue;
                }
                Map<String, Object> inputSchema = null;
                JsonNode schemaNode = node.get("inputSchema");
                if (schemaNode == null || schemaNode.isNull()) {
                    schemaNode = node.get("input_schema");
                }
                if (schemaNode != null && !schemaNode.isNull()) {
                    inputSchema = objectMapper.convertValue(schemaNode, new TypeReference<Map<String, Object>>() {});
                }
                String description = node.path("description").asText(null);
                if (description != null && description.isBlank()) {
                    description = null;
                }
                tools.add(McpDiscoveredToolVO.builder()
                        .name(name)
                        .description(description)
                        .inputSchema(inputSchema)
                        .build());
            }
            return tools;
        } catch (Exception e) {
            return List.of();
        }
    }

    private McpServerVO toVO(McpServerEntity entity) {
        McpServerConfig config = parseStoredConfig(entity.getServerName(), entity.getServerConfig());
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
                .transportType(config.getTransportType())
                .commandSummary(config.commandSummary())
                .serverConfig(entity.getServerConfig())
                .status(status)
                .statusLabel(statusLabel(status))
                .toolCount(toolCount)
                .lastConnectedAt(entity.getLastConnectedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
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
