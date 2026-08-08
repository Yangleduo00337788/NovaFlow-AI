package ai.novaflow.prompt.service;

import ai.novaflow.aiengine.agent.ChatAgentExecutor;
import ai.novaflow.aiengine.agent.ChatExecuteRequest;
import ai.novaflow.aiengine.agent.ChatExecuteResult;
import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.model.domain.ResolvedModelConfig;
import ai.novaflow.model.service.ModelResolutionService;
import ai.novaflow.prompt.domain.PromptVariable;
import ai.novaflow.prompt.domain.dto.PromptTemplateSaveRequest;
import ai.novaflow.prompt.domain.dto.PromptTestRequest;
import ai.novaflow.prompt.domain.vo.PromptTemplateVO;
import ai.novaflow.prompt.domain.vo.PromptTestResultVO;
import ai.novaflow.prompt.domain.vo.PromptVersionVO;
import ai.novaflow.prompt.entity.PromptTemplateEntity;
import ai.novaflow.prompt.entity.PromptTemplateVersionEntity;
import ai.novaflow.prompt.mapper.PromptTemplateMapper;
import ai.novaflow.prompt.mapper.PromptTemplateVersionMapper;
import ai.novaflow.prompt.util.PromptRenderUtils;
import ai.novaflow.prompt.util.PromptVariableUtils;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptTemplateService {

    private final PromptTemplateMapper promptTemplateMapper;
    private final PromptTemplateVersionMapper promptTemplateVersionMapper;
    private final PromptVariableUtils promptVariableUtils;
    private final ModelResolutionService modelResolutionService;
    private final ChatAgentExecutor chatAgentExecutor;

    public PageResult<PromptTemplateVO> page(int page, int pageSize, String keyword, String category) {
        Long tenantId = requireTenantId();
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", 0);
        if (StringUtils.hasText(keyword)) {
            query.and("(template_name like ? or description like ?)",
                    "%" + keyword + "%", "%" + keyword + "%");
        }
        if (StringUtils.hasText(category)) {
            query.eq("category", category);
        }
        query.orderBy("updated_at", false);

        Page<PromptTemplateEntity> result = promptTemplateMapper.paginate(Page.of(page, pageSize), query);
        List<PromptTemplateVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getTotalRow(), page, pageSize);
    }

    public List<PromptTemplateVO> listOptions(String keyword) {
        Long tenantId = requireTenantId();
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", 0)
                .eq("status", 1);
        if (StringUtils.hasText(keyword)) {
            query.and("(template_name like ? or description like ?)",
                    "%" + keyword + "%", "%" + keyword + "%");
        }
        query.orderBy("template_name", true);
        return promptTemplateMapper.selectListByQuery(query).stream().map(this::toVO).toList();
    }

    public PromptTemplateVO detail(Long id) {
        return toVO(getTemplateOrThrow(id));
    }

    public List<PromptVersionVO> listVersions(Long templateId) {
        getTemplateOrThrow(templateId);
        return promptTemplateVersionMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("template_id", templateId)
                        .eq("tenant_id", requireTenantId())
                        .orderBy("version", false)
        ).stream().map(this::toVersionVO).toList();
    }

    @Transactional
    public PromptTemplateVO create(PromptTemplateSaveRequest request) {
        Long tenantId = requireTenantId();
        ensureNameUnique(tenantId, request.getTemplateName(), null);

        LocalDateTime now = LocalDateTime.now();
        Long userId = StpUtil.getLoginIdAsLong();
        String variablesJson = promptVariableUtils.serialize(request.getVariables());

        PromptTemplateEntity entity = new PromptTemplateEntity();
        entity.setTenantId(tenantId);
        entity.setTemplateName(request.getTemplateName().trim());
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setCategory(StringUtils.hasText(request.getCategory()) ? request.getCategory() : "custom");
        entity.setContent(request.getContent().trim());
        entity.setVariables(variablesJson);
        entity.setVisibility(StringUtils.hasText(request.getVisibility()) ? request.getVisibility() : "private");
        entity.setCurrentVersion(1);
        entity.setUsageCount(0);
        entity.setStatus(1);
        entity.setCreatedBy(userId);
        entity.setIsDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        promptTemplateMapper.insert(entity);

        insertVersion(entity, 1, request.getChangeLog(), userId, now);
        return toVO(entity);
    }

    @Transactional
    public PromptTemplateVO update(Long id, PromptTemplateSaveRequest request) {
        PromptTemplateEntity entity = getTemplateOrThrow(id);
        ensureNameUnique(entity.getTenantId(), request.getTemplateName(), id);

        boolean contentChanged = !Objects.equals(entity.getContent(), request.getContent().trim())
                || !Objects.equals(entity.getVariables(), promptVariableUtils.serialize(request.getVariables()));

        entity.setTemplateName(request.getTemplateName().trim());
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setCategory(StringUtils.hasText(request.getCategory()) ? request.getCategory() : entity.getCategory());
        entity.setContent(request.getContent().trim());
        entity.setVariables(promptVariableUtils.serialize(request.getVariables()));
        entity.setVisibility(StringUtils.hasText(request.getVisibility()) ? request.getVisibility() : entity.getVisibility());
        entity.setUpdatedAt(LocalDateTime.now());

        if (contentChanged) {
            int nextVersion = getNextVersionNumber(entity.getId());
            entity.setCurrentVersion(nextVersion);
            insertVersion(entity, nextVersion, request.getChangeLog(), StpUtil.getLoginIdAsLong(), LocalDateTime.now());
        }

        promptTemplateMapper.update(entity);
        return toVO(entity);
    }

    @Transactional
    public PromptTemplateVO rollback(Long id, Integer version) {
        PromptTemplateEntity entity = getTemplateOrThrow(id);
        PromptTemplateVersionEntity versionEntity = promptTemplateVersionMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("template_id", id)
                        .eq("tenant_id", entity.getTenantId())
                        .eq("version", version)
        );
        if (versionEntity == null) {
            throw new BusinessException("指定版本不存在");
        }

        entity.setContent(versionEntity.getContent());
        entity.setVariables(versionEntity.getVariables());
        entity.setCurrentVersion(version);
        entity.setUpdatedAt(LocalDateTime.now());
        promptTemplateMapper.update(entity);
        return toVO(entity);
    }

    @Transactional
    public void delete(Long id) {
        PromptTemplateEntity entity = getTemplateOrThrow(id);
        entity.setIsDeleted(1);
        entity.setUpdatedAt(LocalDateTime.now());
        promptTemplateMapper.update(entity);
    }

    public PromptTestResultVO test(Long id, PromptTestRequest request) {
        PromptTemplateEntity entity = getTemplateOrThrow(id);
        Map<String, Object> variables = buildDefaultVariableMap(
                promptVariableUtils.parse(entity.getVariables()));
        enrichBuiltinVariables(variables, entity);
        if (request != null && request.getVariables() != null) {
            variables.putAll(request.getVariables());
        }
        String renderedPrompt = PromptRenderUtils.render(entity.getContent(), variables);

        PromptTestResultVO.PromptTestResultVOBuilder builder = PromptTestResultVO.builder()
                .renderedPrompt(renderedPrompt);

        if (request == null || request.getModelConfigId() == null) {
            return builder.build();
        }
        if (!StringUtils.hasText(request.getUserMessage())) {
            throw new BusinessException("在线测试需填写用户消息");
        }

        ResolvedModelConfig modelConfig = modelResolutionService.resolve(
                request.getModelConfigId(),
                entity.getTenantId()
        );
        long start = System.currentTimeMillis();
        try {
            ChatExecuteResult result = chatAgentExecutor.execute(ChatExecuteRequest.builder()
                    .modelConfig(modelConfig)
                    .systemPrompt(renderedPrompt)
                    .userMessage(request.getUserMessage().trim())
                    .memoryWindow(0)
                    .build());

            return builder
                    .reply(result.getReply())
                    .tokensUsed(result.getTokensUsed())
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Prompt test failed, templateId={}, modelConfigId={}", id, request.getModelConfigId(), e);
            throw new BusinessException("模型调用失败: " + rootMessage(e));
        }
    }

    public String resolveContent(Long tenantId, Long templateId, Long versionId) {
        if (templateId == null) {
            return null;
        }
        PromptTemplateEntity entity = promptTemplateMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", templateId)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
        );
        if (entity == null) {
            throw new BusinessException("Prompt 模板不存在");
        }
        if (versionId == null) {
            return entity.getContent();
        }
        PromptTemplateVersionEntity versionEntity = promptTemplateVersionMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", versionId)
                        .eq("template_id", templateId)
                        .eq("tenant_id", tenantId)
        );
        return versionEntity != null ? versionEntity.getContent() : entity.getContent();
    }

    public String resolveRenderedContent(Long tenantId, Long templateId, Long versionId) {
        String raw = resolveContent(tenantId, templateId, versionId);
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        return renderWithTemplateDefaults(tenantId, templateId, versionId, raw);
    }

    public String renderWithTemplateDefaults(Long tenantId, Long templateId, Long versionId, String content) {
        if (!StringUtils.hasText(content)) {
            return content;
        }
        PromptTemplateEntity entity = loadTemplateEntity(tenantId, templateId);
        Map<String, Object> variables = buildDefaultVariableMap(loadTemplateVariables(tenantId, templateId, versionId));
        enrichBuiltinVariables(variables, entity);
        return PromptRenderUtils.render(content, variables);
    }

    public Integer getCurrentVersion(Long tenantId, Long templateId) {
        PromptTemplateEntity entity = loadTemplateEntity(tenantId, templateId);
        return entity != null ? entity.getCurrentVersion() : null;
    }

    public void incrementUsageCount(Long templateId) {
        if (templateId == null) {
            return;
        }
        PromptTemplateEntity entity = promptTemplateMapper.selectOneById(templateId);
        if (entity == null || entity.getIsDeleted() != null && entity.getIsDeleted() == 1) {
            return;
        }
        entity.setUsageCount((entity.getUsageCount() != null ? entity.getUsageCount() : 0) + 1);
        entity.setUpdatedAt(LocalDateTime.now());
        promptTemplateMapper.update(entity);
    }

    public PromptTemplateEntity getTemplateOrThrow(Long id) {
        PromptTemplateEntity entity = promptTemplateMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", id)
                        .eq("tenant_id", requireTenantId())
                        .eq("is_deleted", 0)
        );
        if (entity == null) {
            throw new BusinessException("Prompt 模板不存在");
        }
        return entity;
    }

    private int getNextVersionNumber(Long templateId) {
        PromptTemplateVersionEntity latest = promptTemplateVersionMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("template_id", templateId)
                        .orderBy("version", false)
                        .limit(1)
        );
        return latest != null ? latest.getVersion() + 1 : 1;
    }

    private List<PromptVariable> loadTemplateVariables(Long tenantId, Long templateId, Long versionId) {
        if (versionId != null) {
            PromptTemplateVersionEntity versionEntity = promptTemplateVersionMapper.selectOneByQuery(
                    QueryWrapper.create()
                            .eq("id", versionId)
                            .eq("template_id", templateId)
                            .eq("tenant_id", tenantId)
            );
            if (versionEntity != null) {
                return promptVariableUtils.parse(versionEntity.getVariables());
            }
        }
        PromptTemplateEntity entity = promptTemplateMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", templateId)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
        );
        return entity != null ? promptVariableUtils.parse(entity.getVariables()) : List.of();
    }

    private PromptTemplateEntity loadTemplateEntity(Long tenantId, Long templateId) {
        if (templateId == null) {
            return null;
        }
        return promptTemplateMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", templateId)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
        );
    }

    private void enrichBuiltinVariables(Map<String, Object> variables, PromptTemplateEntity entity) {
        if (variables == null || entity == null || entity.getCurrentVersion() == null) {
            return;
        }
        variables.putIfAbsent("template_version", String.valueOf(entity.getCurrentVersion()));
    }

    private Map<String, Object> buildDefaultVariableMap(List<PromptVariable> variables) {
        Map<String, Object> defaults = new LinkedHashMap<>();
        if (variables == null) {
            return defaults;
        }
        for (PromptVariable variable : variables) {
            if (variable != null
                    && StringUtils.hasText(variable.getName())
                    && StringUtils.hasText(variable.getDefaultValue())) {
                defaults.put(variable.getName().trim(), variable.getDefaultValue().trim());
            }
        }
        return defaults;
    }

    private void insertVersion(
            PromptTemplateEntity entity,
            int version,
            String changeLog,
            Long userId,
            LocalDateTime now) {
        PromptTemplateVersionEntity versionEntity = new PromptTemplateVersionEntity();
        versionEntity.setTenantId(entity.getTenantId());
        versionEntity.setTemplateId(entity.getId());
        versionEntity.setVersion(version);
        versionEntity.setContent(entity.getContent());
        versionEntity.setVariables(entity.getVariables());
        versionEntity.setChangeLog(trimToNull(changeLog));
        versionEntity.setPublishedBy(userId);
        versionEntity.setPublishedAt(now);
        promptTemplateVersionMapper.insert(versionEntity);
    }

    private void ensureNameUnique(Long tenantId, String templateName, Long excludeId) {
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("template_name", templateName.trim())
                .eq("is_deleted", 0);
        if (excludeId != null) {
            query.ne("id", excludeId);
        }
        if (promptTemplateMapper.selectCountByQuery(query) > 0) {
            throw new BusinessException("模板名称已存在");
        }
    }

    private PromptTemplateVO toVO(PromptTemplateEntity entity) {
        return PromptTemplateVO.builder()
                .id(entity.getId())
                .templateName(entity.getTemplateName())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .content(entity.getContent())
                .variables(promptVariableUtils.parse(entity.getVariables()))
                .visibility(entity.getVisibility())
                .currentVersion(entity.getCurrentVersion())
                .usageCount(entity.getUsageCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private PromptVersionVO toVersionVO(PromptTemplateVersionEntity entity) {
        return PromptVersionVO.builder()
                .id(entity.getId())
                .templateId(entity.getTemplateId())
                .version(entity.getVersion())
                .content(entity.getContent())
                .variables(promptVariableUtils.parse(entity.getVariables()))
                .changeLog(entity.getChangeLog())
                .publishedAt(entity.getPublishedAt())
                .build();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : "未知错误";
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户上下文缺失");
        }
        return tenantId;
    }
}
