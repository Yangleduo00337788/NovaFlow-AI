package ai.novaflow.tool.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.tool.domain.HttpToolDefinition;
import ai.novaflow.tool.domain.McpToolDefinition;
import ai.novaflow.tool.domain.dto.ToolDefinitionSaveRequest;
import ai.novaflow.tool.domain.dto.ToolTestRequest;
import ai.novaflow.tool.domain.vo.ToolDefinitionVO;
import ai.novaflow.tool.domain.vo.ToolTestResultVO;
import ai.novaflow.tool.entity.ToolDefinitionEntity;
import ai.novaflow.tool.executor.ToolExecutorRouter;
import ai.novaflow.tool.mapper.ToolDefinitionMapper;
import ai.novaflow.tool.util.ToolConfigConverter;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ToolDefinitionService {

    private final ToolDefinitionMapper toolDefinitionMapper;
    private final ToolConfigConverter toolConfigConverter;
    private final ToolExecutorRouter toolExecutorRouter;

    public PageResult<ToolDefinitionVO> page(int page, int pageSize, String keyword) {
        Long tenantId = requireTenantId();
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", 0);
        if (StringUtils.hasText(keyword)) {
            query.and("(tool_name like ? or display_name like ? or description like ?)",
                    "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%");
        }
        query.orderBy("updated_at", false);

        Page<ToolDefinitionEntity> result = toolDefinitionMapper.paginate(Page.of(page, pageSize), query);
        List<ToolDefinitionVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getTotalRow(), page, pageSize);
    }

    public List<ToolDefinitionVO> listEnabled(String keyword) {
        Long tenantId = requireTenantId();
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", 0)
                .eq("is_enabled", 1);
        if (StringUtils.hasText(keyword)) {
            query.and("(tool_name like ? or display_name like ?)",
                    "%" + keyword + "%", "%" + keyword + "%");
        }
        query.orderBy("display_name", true);
        return toolDefinitionMapper.selectListByQuery(query).stream().map(this::toVO).toList();
    }

    public ToolDefinitionVO detail(Long id) {
        return toVO(getToolOrThrow(id));
    }

    @Transactional
    public ToolDefinitionVO create(ToolDefinitionSaveRequest request) {
        Long tenantId = requireTenantId();
        ensureToolNameUnique(tenantId, request.getToolName(), null);
        validateHttpTool(request.toHttpToolDefinition());

        LocalDateTime now = LocalDateTime.now();
        ToolDefinitionEntity entity = new ToolDefinitionEntity();
        entity.setTenantId(tenantId);
        entity.setToolName(request.getToolName().trim());
        entity.setDisplayName(request.getDisplayName().trim());
        entity.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : "");
        entity.setToolType(StringUtils.hasText(request.getToolType()) ? request.getToolType() : "http");
        entity.setToolConfig(toolConfigConverter.serializeConfig(request.toHttpToolDefinition()));
        entity.setIsEnabled(1);
        entity.setIsPublic(0);
        entity.setCreatedBy(StpUtil.getLoginIdAsLong());
        entity.setIsDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        toolDefinitionMapper.insert(entity);
        return toVO(entity);
    }

    @Transactional
    public ToolDefinitionVO update(Long id, ToolDefinitionSaveRequest request) {
        ToolDefinitionEntity entity = getToolOrThrow(id);
        ensureToolNameUnique(entity.getTenantId(), request.getToolName(), id);
        validateHttpTool(request.toHttpToolDefinition());

        entity.setToolName(request.getToolName().trim());
        entity.setDisplayName(request.getDisplayName().trim());
        entity.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : "");
        entity.setToolType(StringUtils.hasText(request.getToolType()) ? request.getToolType() : "http");
        entity.setToolConfig(toolConfigConverter.serializeConfig(request.toHttpToolDefinition()));
        entity.setUpdatedAt(LocalDateTime.now());
        toolDefinitionMapper.update(entity);
        return toVO(entity);
    }

    @Transactional
    public void delete(Long id) {
        ToolDefinitionEntity entity = getToolOrThrow(id);
        entity.setIsDeleted(1);
        entity.setUpdatedAt(LocalDateTime.now());
        toolDefinitionMapper.update(entity);
    }

    public ToolTestResultVO test(Long id, ToolTestRequest request) {
        ToolDefinitionEntity entity = getToolOrThrow(id);
        if ("mcp".equalsIgnoreCase(entity.getToolType())) {
            HttpToolDefinition tool = toolConfigConverter.toHttpTool(entity);
            Map<String, Object> arguments = request != null && request.getArguments() != null
                    ? request.getArguments()
                    : new HashMap<>();
            try {
                String result = toolExecutorRouter.execute(tool, arguments);
                return ToolTestResultVO.builder()
                        .success(true)
                        .result(result)
                        .build();
            } catch (Exception e) {
                return ToolTestResultVO.builder()
                        .success(false)
                        .error(e.getMessage())
                        .build();
            }
        }
        HttpToolDefinition tool = toolConfigConverter.toHttpTool(entity);
        Map<String, Object> arguments = request != null && request.getArguments() != null
                ? request.getArguments()
                : new HashMap<>();
        try {
            String result = toolExecutorRouter.execute(tool, arguments);
            return ToolTestResultVO.builder()
                    .success(true)
                    .result(result)
                    .build();
        } catch (Exception e) {
            return ToolTestResultVO.builder()
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    public List<HttpToolDefinition> resolveTools(Long tenantId, List<Long> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return List.of();
        }
        List<Long> uniqueIds = toolIds.stream().distinct().toList();
        List<ToolDefinitionEntity> entities = toolDefinitionMapper.selectListByQuery(
                QueryWrapper.create()
                        .in("id", uniqueIds)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
                        .eq("is_enabled", 1)
        );
        Map<Long, ToolDefinitionEntity> entityMap = new HashMap<>();
        for (ToolDefinitionEntity entity : entities) {
            entityMap.put(entity.getId(), entity);
        }
        List<HttpToolDefinition> tools = new ArrayList<>();
        for (Long toolId : uniqueIds) {
            ToolDefinitionEntity entity = entityMap.get(toolId);
            if (entity != null) {
                tools.add(toolConfigConverter.toHttpTool(entity));
            }
        }
        return tools;
    }

    private void validateHttpTool(HttpToolDefinition tool) {
        if (!StringUtils.hasText(tool.getUrl())) {
            throw new BusinessException("请求 URL 不能为空");
        }
    }

    private void ensureToolNameUnique(Long tenantId, String toolName, Long excludeId) {
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("tool_name", toolName.trim())
                .eq("is_deleted", 0);
        if (excludeId != null) {
            query.ne("id", excludeId);
        }
        if (toolDefinitionMapper.selectCountByQuery(query) > 0) {
            throw new BusinessException("工具标识已存在");
        }
    }

    public ToolDefinitionEntity getToolOrThrow(Long id) {
        ToolDefinitionEntity entity = toolDefinitionMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", id)
                        .eq("tenant_id", requireTenantId())
                        .eq("is_deleted", 0)
        );
        if (entity == null) {
            throw new BusinessException("工具不存在");
        }
        return entity;
    }

    private ToolDefinitionVO toVO(ToolDefinitionEntity entity) {
        ToolDefinitionVO.ToolDefinitionVOBuilder builder = ToolDefinitionVO.builder()
                .id(entity.getId())
                .toolName(entity.getToolName())
                .displayName(entity.getDisplayName())
                .description(entity.getDescription())
                .toolType(entity.getToolType())
                .enabled(entity.getIsEnabled() != null && entity.getIsEnabled() == 1)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());
        if ("mcp".equalsIgnoreCase(entity.getToolType())) {
            McpToolDefinition mcpTool = toolConfigConverter.toMcpTool(entity);
            builder.mcpServerId(mcpTool.getMcpServerId())
                    .mcpToolName(mcpTool.getMcpToolName())
                    .sourceServerName(mcpTool.getSourceServerName())
                    .inputSchema(mcpTool.getInputSchema());
            return builder.build();
        }
        HttpToolDefinition tool = toolConfigConverter.toHttpTool(entity);
        return builder
                .method(tool.getMethod())
                .url(tool.getUrl())
                .bodyTemplate(tool.getBodyTemplate())
                .headers(tool.getHeaders())
                .inputSchema(tool.getInputSchema())
                .build();
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户上下文缺失");
        }
        return tenantId;
    }
}
