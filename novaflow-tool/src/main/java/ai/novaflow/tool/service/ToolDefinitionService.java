package ai.novaflow.tool.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.tool.domain.HttpToolDefinition;
import ai.novaflow.tool.domain.McpToolDefinition;
import ai.novaflow.tool.domain.SkillDefinition;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ToolDefinitionService {

    private static final int SKILL_MAX_BYTES = 512 * 1024;

    private final ToolDefinitionMapper toolDefinitionMapper;
    private final ToolConfigConverter toolConfigConverter;
    private final ToolExecutorRouter toolExecutorRouter;

    public PageResult<ToolDefinitionVO> page(int page, int pageSize, String keyword, String toolType) {
        Long tenantId = requireTenantId();
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", 0);
        if (StringUtils.hasText(keyword)) {
            query.and("(tool_name like ? or display_name like ? or description like ?)",
                    "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%");
        }
        applyToolTypeFilter(query, toolType);
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
        query.and("(tool_type is null or tool_type = '' or tool_type in ('http', 'mcp'))");
        query.orderBy("display_name", true);
        return toolDefinitionMapper.selectListByQuery(query).stream().map(this::toVO).toList();
    }

    public List<ToolDefinitionVO> listSkillOptions(String keyword) {
        Long tenantId = requireTenantId();
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", 0)
                .eq("is_enabled", 1)
                .eq("tool_type", "skill");
        if (StringUtils.hasText(keyword)) {
            query.and("(tool_name like ? or display_name like ? or description like ?)",
                    "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%");
        }
        query.orderBy("display_name", true);
        return toolDefinitionMapper.selectListByQuery(query).stream().map(this::toVO).toList();
    }

    public List<SkillDefinition> resolveSkills(Long tenantId, List<Long> skillIds) {
        if (skillIds == null || skillIds.isEmpty() || tenantId == null) {
            return List.of();
        }
        List<Long> uniqueIds = skillIds.stream().distinct().toList();
        List<ToolDefinitionEntity> entities = toolDefinitionMapper.selectListByQuery(
                QueryWrapper.create()
                        .in("id", uniqueIds)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
                        .eq("is_enabled", 1)
                        .eq("tool_type", "skill")
        );
        Map<Long, ToolDefinitionEntity> entityMap = new HashMap<>();
        for (ToolDefinitionEntity entity : entities) {
            entityMap.put(entity.getId(), entity);
        }
        List<SkillDefinition> skills = new ArrayList<>();
        for (Long skillId : uniqueIds) {
            ToolDefinitionEntity entity = entityMap.get(skillId);
            if (entity == null) {
                continue;
            }
            SkillDefinition skill = toolConfigConverter.toSkill(entity);
            skill.setToolName(entity.getToolName());
            skill.setDisplayName(entity.getDisplayName());
            skills.add(skill);
        }
        return skills;
    }

    public String buildSkillSystemPromptBlock(Long tenantId, List<Long> skillIds) {
        List<SkillDefinition> skills = resolveSkills(tenantId, skillIds);
        if (skills.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("【Agent Skills】\n");
        builder.append("以下技能为指导性知识，请在回复中遵循相关流程与规范。\n");
        for (int i = 0; i < skills.size(); i++) {
            SkillDefinition skill = skills.get(i);
            builder.append("\n--- Skill ").append(i + 1);
            if (StringUtils.hasText(skill.getDisplayName())) {
                builder.append(": ").append(skill.getDisplayName().trim());
            }
            if (StringUtils.hasText(skill.getFileName())) {
                builder.append(" (").append(skill.getFileName().trim()).append(")");
            }
            builder.append(" ---\n");
            if (StringUtils.hasText(skill.getContent())) {
                builder.append(skill.getContent().trim());
            }
        }
        return builder.toString();
    }

    public void ensureCallableTool(Long toolId) {
        ToolDefinitionEntity entity = getToolOrThrow(toolId);
        if ("skill".equalsIgnoreCase(entity.getToolType())) {
            throw new BusinessException("Skill 技能请通过「关联技能」配置，不能作为工具关联");
        }
    }

    public void ensureSkill(Long skillId) {
        ToolDefinitionEntity entity = getToolOrThrow(skillId);
        if (!"skill".equalsIgnoreCase(entity.getToolType())) {
            throw new BusinessException("仅可关联 Skill 类型的技能");
        }
    }

    public ToolDefinitionVO detail(Long id) {
        ToolDefinitionEntity entity = getToolOrThrow(id);
        ToolDefinitionVO vo = toVO(entity);
        if ("skill".equalsIgnoreCase(entity.getToolType())) {
            SkillDefinition skill = toolConfigConverter.toSkill(entity);
            vo.setSkillContent(skill.getContent());
        }
        return vo;
    }

    @Transactional
    public ToolDefinitionVO uploadSkill(MultipartFile file) {
        ParsedSkill parsed = parseSkillFile(file);
        Long tenantId = requireTenantId();
        ensureToolNameUnique(tenantId, parsed.toolName(), null);

        LocalDateTime now = LocalDateTime.now();
        ToolDefinitionEntity entity = new ToolDefinitionEntity();
        entity.setTenantId(tenantId);
        entity.setToolName(parsed.toolName());
        entity.setDisplayName(parsed.displayName());
        entity.setDescription(parsed.description());
        entity.setToolType("skill");
        entity.setToolConfig(toolConfigConverter.serializeSkill(SkillDefinition.builder()
                .fileName(parsed.fileName())
                .content(parsed.content())
                .build()));
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
    public ToolDefinitionVO reuploadSkill(Long id, MultipartFile file) {
        ToolDefinitionEntity entity = getToolOrThrow(id);
        if (!"skill".equalsIgnoreCase(entity.getToolType())) {
            throw new BusinessException("仅 Skill 类型支持重新上传");
        }
        ParsedSkill parsed = parseSkillFile(file);
        if (!parsed.toolName().equals(entity.getToolName())) {
            ensureToolNameUnique(entity.getTenantId(), parsed.toolName(), id);
            entity.setToolName(parsed.toolName());
        }
        entity.setDisplayName(parsed.displayName());
        entity.setDescription(parsed.description());
        entity.setToolConfig(toolConfigConverter.serializeSkill(SkillDefinition.builder()
                .fileName(parsed.fileName())
                .content(parsed.content())
                .build()));
        entity.setUpdatedAt(LocalDateTime.now());
        toolDefinitionMapper.update(entity);
        ToolDefinitionVO vo = toVO(entity);
        vo.setSkillContent(parsed.content());
        return vo;
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
            if (entity != null && !"skill".equalsIgnoreCase(entity.getToolType())) {
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
        if ("skill".equalsIgnoreCase(entity.getToolType())) {
            SkillDefinition skill = toolConfigConverter.toSkill(entity);
            String preview = skill.getContent();
            if (preview != null && preview.length() > 200) {
                preview = preview.substring(0, 200) + "...";
            }
            builder.skillFileName(skill.getFileName())
                    .skillContentPreview(preview);
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

    private void applyToolTypeFilter(QueryWrapper query, String toolType) {
        if (!StringUtils.hasText(toolType)) {
            return;
        }
        String normalized = toolType.trim();
        if ("http".equalsIgnoreCase(normalized)) {
            query.and("(tool_type is null or tool_type = '' or tool_type = 'http')");
            return;
        }
        query.eq("tool_type", normalized);
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户上下文缺失");
        }
        return tenantId;
    }

    private ParsedSkill parseSkillFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传 SKILL.md 文件");
        }
        if (file.getSize() > SKILL_MAX_BYTES) {
            throw new BusinessException("Skill 文件不能超过 512KB");
        }
        String originalName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalName) || !originalName.toLowerCase().endsWith(".md")) {
            throw new BusinessException("仅支持 .md 格式的 Skill 文件");
        }
        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException("读取 Skill 文件失败");
        }
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("Skill 文件内容不能为空");
        }
        SkillMetadata metadata = parseSkillMetadata(content, originalName);
        return new ParsedSkill(
                originalName,
                metadata.toolName(),
                metadata.displayName(),
                metadata.description(),
                content
        );
    }

    private SkillMetadata parseSkillMetadata(String content, String fileName) {
        String toolName = sanitizeToolName(fileName);
        String displayName = toolName.replace('_', ' ');
        String description = "";

        if (content.startsWith("---")) {
            int end = content.indexOf("---", 3);
            if (end > 0) {
                String frontmatter = content.substring(3, end);
                for (String line : frontmatter.split("\\r?\\n")) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("name:")) {
                        String nameValue = trimmed.substring(5).trim();
                        toolName = sanitizeToolName(nameValue);
                        displayName = nameValue;
                    } else if (trimmed.startsWith("description:")) {
                        description = trimmed.substring(12).trim();
                    }
                }
            }
        }
        if (!StringUtils.hasText(displayName)) {
            displayName = toolName.replace('_', ' ');
        }
        return new SkillMetadata(toolName, displayName, description);
    }

    private String sanitizeToolName(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "skill";
        }
        String name = raw.trim().toLowerCase()
                .replaceAll("\\.md$", "")
                .replaceAll("[^a-z0-9_\\-]", "_")
                .replaceAll("_+", "_");
        while (name.startsWith("_")) {
            name = name.substring(1);
        }
        while (name.endsWith("_")) {
            name = name.substring(0, name.length() - 1);
        }
        return StringUtils.hasText(name) ? name : "skill";
    }

    private record SkillMetadata(String toolName, String displayName, String description) {}

    private record ParsedSkill(
            String fileName,
            String toolName,
            String displayName,
            String description,
            String content
    ) {}
}
