package ai.novaflow.tool.service;

import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.tool.domain.McpToolDefinition;
import ai.novaflow.tool.domain.vo.McpSyncResultVO;
import ai.novaflow.tool.entity.McpServerEntity;
import ai.novaflow.tool.entity.ToolDefinitionEntity;
import ai.novaflow.tool.mapper.ToolDefinitionMapper;
import ai.novaflow.tool.mcp.McpDiscoveredTool;
import ai.novaflow.tool.util.ToolConfigConverter;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class McpToolSyncService {

    private final ToolDefinitionMapper toolDefinitionMapper;
    private final ToolConfigConverter toolConfigConverter;

    @Transactional
    public McpSyncResultVO sync(McpServerEntity server, List<McpDiscoveredTool> discoveredTools) {
        if (discoveredTools == null || discoveredTools.isEmpty()) {
            throw new BusinessException("暂无已发现工具，请先执行连接测试");
        }

        Long tenantId = server.getTenantId();
        Long mcpServerId = server.getId();
        Map<String, ToolDefinitionEntity> existingByMcpToolName = loadExistingTools(tenantId, mcpServerId);

        int created = 0;
        int updated = 0;
        Set<String> syncedNames = new HashSet<>();
        List<String> toolNames = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;

        for (McpDiscoveredTool discoveredTool : discoveredTools) {
            if (!StringUtils.hasText(discoveredTool.getName())) {
                continue;
            }
            String mcpToolName = discoveredTool.getName().trim();
            syncedNames.add(mcpToolName);
            toolNames.add(mcpToolName);

            McpToolDefinition mcpToolDefinition = McpToolDefinition.builder()
                    .mcpServerId(mcpServerId)
                    .mcpToolName(mcpToolName)
                    .sourceServerName(server.getServerName())
                    .inputSchema(discoveredTool.getInputSchema())
                    .build();

            ToolDefinitionEntity existing = existingByMcpToolName.get(mcpToolName);
            if (existing == null) {
                ToolDefinitionEntity entity = new ToolDefinitionEntity();
                entity.setTenantId(tenantId);
                entity.setToolName(buildToolName(tenantId, mcpServerId, mcpToolName));
                entity.setDisplayName(buildDisplayName(server.getServerName(), mcpToolName));
                entity.setDescription(StringUtils.hasText(discoveredTool.getDescription())
                        ? discoveredTool.getDescription().trim()
                        : mcpToolName);
                entity.setToolType("mcp");
                entity.setToolConfig(toolConfigConverter.serializeMcpConfig(mcpToolDefinition));
                entity.setIsEnabled(1);
                entity.setIsPublic(0);
                entity.setCreatedBy(userId);
                entity.setIsDeleted(0);
                entity.setCreatedAt(now);
                entity.setUpdatedAt(now);
                toolDefinitionMapper.insert(entity);
                created++;
            } else {
                existing.setDisplayName(buildDisplayName(server.getServerName(), mcpToolName));
                existing.setDescription(StringUtils.hasText(discoveredTool.getDescription())
                        ? discoveredTool.getDescription().trim()
                        : mcpToolName);
                existing.setToolConfig(toolConfigConverter.serializeMcpConfig(mcpToolDefinition));
                existing.setUpdatedAt(now);
                toolDefinitionMapper.update(existing);
                updated++;
            }
        }

        int removed = 0;
        for (Map.Entry<String, ToolDefinitionEntity> entry : existingByMcpToolName.entrySet()) {
            if (!syncedNames.contains(entry.getKey())) {
                ToolDefinitionEntity entity = entry.getValue();
                entity.setIsDeleted(1);
                entity.setUpdatedAt(now);
                toolDefinitionMapper.update(entity);
                removed++;
            }
        }

        int syncedToolCount = countSyncedTools(tenantId, mcpServerId);
        return McpSyncResultVO.builder()
                .mcpServerId(mcpServerId)
                .serverName(server.getServerName())
                .created(created)
                .updated(updated)
                .removed(removed)
                .total(syncedNames.size())
                .syncedToolCount(syncedToolCount)
                .toolNames(toolNames)
                .message(String.format("同步完成：新增 %d、更新 %d、移除 %d", created, updated, removed))
                .build();
    }

    public int countSyncedTools(Long tenantId, Long mcpServerId) {
        return loadExistingTools(tenantId, mcpServerId).size();
    }

    private Map<String, ToolDefinitionEntity> loadExistingTools(Long tenantId, Long mcpServerId) {
        List<ToolDefinitionEntity> entities = toolDefinitionMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("tool_type", "mcp")
                        .eq("is_deleted", 0)
        );
        Map<String, ToolDefinitionEntity> result = new HashMap<>();
        for (ToolDefinitionEntity entity : entities) {
            McpToolDefinition mcpTool = toolConfigConverter.toMcpTool(entity);
            if (mcpServerId.equals(mcpTool.getMcpServerId()) && StringUtils.hasText(mcpTool.getMcpToolName())) {
                result.put(mcpTool.getMcpToolName(), entity);
            }
        }
        return result;
    }

    private String buildDisplayName(String serverName, String mcpToolName) {
        if (StringUtils.hasText(serverName)) {
            return serverName.trim() + " · " + mcpToolName;
        }
        return mcpToolName;
    }

    private String buildToolName(Long tenantId, Long mcpServerId, String mcpToolName) {
        String sanitized = mcpToolName.toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (!StringUtils.hasText(sanitized)) {
            sanitized = "tool";
        }
        String toolName = "mcp_" + mcpServerId + "_" + sanitized;
        if (toolName.length() > 64) {
            toolName = toolName.substring(0, 64);
        }
        if (!toolName.matches("^[a-z].*")) {
            toolName = "mcp_" + mcpServerId + "_tool";
        }
        return ensureUniqueToolName(tenantId, toolName, mcpServerId);
    }

    private String ensureUniqueToolName(Long tenantId, String baseName, Long mcpServerId) {
        String candidate = baseName;
        int suffix = 1;
        while (true) {
            ToolDefinitionEntity existing = toolDefinitionMapper.selectOneByQuery(
                    QueryWrapper.create()
                            .eq("tenant_id", tenantId)
                            .eq("tool_name", candidate)
                            .eq("is_deleted", 0)
            );
            if (existing == null) {
                return candidate;
            }
            McpToolDefinition mcpTool = toolConfigConverter.toMcpTool(existing);
            if (mcpServerId.equals(mcpTool.getMcpServerId())) {
                return candidate;
            }
            candidate = baseName + "_" + suffix++;
            if (candidate.length() > 64) {
                candidate = baseName.substring(0, Math.max(1, 64 - String.valueOf(suffix).length() - 1)) + "_" + suffix;
            }
        }
    }
}
