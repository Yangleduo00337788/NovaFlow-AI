package ai.novaflow.model.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.model.domain.BillingCurrency;
import ai.novaflow.model.domain.dto.ModelConfigSaveRequest;
import ai.novaflow.model.domain.vo.ModelConfigVO;
import ai.novaflow.model.entity.ModelConfigEntity;
import ai.novaflow.model.entity.ModelProviderEntity;
import ai.novaflow.model.mapper.ModelConfigMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelConfigService {

    private final ModelConfigMapper modelConfigMapper;
    private final ModelProviderService modelProviderService;

    public List<ModelConfigVO> list(Long providerId, String modelType) {
        Long tenantId = requireTenantId();
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", 0);
        if (providerId != null) {
            query.eq("provider_id", providerId);
        }
        if (StringUtils.hasText(modelType)) {
            query.eq("model_type", modelType);
        }
        query.orderBy("is_default", false).orderBy("updated_at", false);

        return modelConfigMapper.selectListByQuery(query).stream()
                .map(this::toVO)
                .toList();
    }

    public ModelConfigVO detail(Long id) {
        return toVO(getConfigOrThrow(id));
    }

    @Transactional
    public ModelConfigVO create(ModelConfigSaveRequest request) {
        ModelProviderEntity provider = modelProviderService.getProviderOrThrow(request.getProviderId());
        ensureModelUnique(provider.getId(), request.getModelName(), request.getModelType(), null);

        ModelConfigEntity entity = new ModelConfigEntity();
        entity.setTenantId(provider.getTenantId());
        entity.setProviderId(provider.getId());
        applyRequest(entity, request);
        entity.setIsDeleted(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        modelConfigMapper.insert(entity);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefault(entity.getTenantId(), entity.getModelType(), entity.getId());
            entity.setIsDefault(1);
            modelConfigMapper.update(entity);
        }

        return toVO(entity);
    }

    @Transactional
    public ModelConfigVO update(Long id, ModelConfigSaveRequest request) {
        ModelConfigEntity entity = getConfigOrThrow(id);
        if (!entity.getProviderId().equals(request.getProviderId())) {
            throw new BusinessException("不允许修改模型所属提供商");
        }
        ensureModelUnique(entity.getProviderId(), request.getModelName(), request.getModelType(), id);
        applyRequest(entity, request);
        entity.setUpdatedAt(LocalDateTime.now());
        modelConfigMapper.update(entity);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            setDefault(id);
            entity = getConfigOrThrow(id);
        }

        return toVO(entity);
    }

    @Transactional
    public void delete(Long id) {
        ModelConfigEntity entity = getConfigOrThrow(id);
        entity.setIsDeleted(1);
        entity.setUpdatedAt(LocalDateTime.now());
        modelConfigMapper.update(entity);
    }

    @Transactional
    public ModelConfigVO setDefault(Long id) {
        ModelConfigEntity entity = getConfigOrThrow(id);
        clearDefault(entity.getTenantId(), entity.getModelType(), id);
        entity.setIsDefault(1);
        entity.setUpdatedAt(LocalDateTime.now());
        modelConfigMapper.update(entity);
        return toVO(entity);
    }

    private void applyRequest(ModelConfigEntity entity, ModelConfigSaveRequest request) {
        entity.setModelName(request.getModelName().trim());
        entity.setModelType(request.getModelType());
        entity.setDisplayName(request.getDisplayName().trim());
        entity.setContextWindow(request.getContextWindow() != null ? request.getContextWindow() : 4096);
        entity.setMaxOutputTokens(request.getMaxOutputTokens() != null ? request.getMaxOutputTokens() : 2048);
        entity.setInputPrice(request.getInputPrice());
        entity.setOutputPrice(request.getOutputPrice());
        entity.setDefaultTemperature(request.getDefaultTemperature() != null
                ? request.getDefaultTemperature()
                : new BigDecimal("0.70"));
        entity.setIsEnabled(request.getEnabled() == null || request.getEnabled() ? 1 : 0);
        if (request.getIsDefault() != null) {
            entity.setIsDefault(request.getIsDefault() ? 1 : 0);
        }
    }

    private void ensureModelUnique(Long providerId, String modelName, String modelType, Long excludeId) {
        QueryWrapper query = QueryWrapper.create()
                .eq("provider_id", providerId)
                .eq("model_name", modelName.trim())
                .eq("model_type", modelType)
                .eq("is_deleted", 0);
        if (excludeId != null) {
            query.ne("id", excludeId);
        }
        if (modelConfigMapper.selectCountByQuery(query) > 0) {
            throw new BusinessException("模型已存在");
        }
    }

    private void clearDefault(Long tenantId, String modelType, Long keepId) {
        List<ModelConfigEntity> defaults = modelConfigMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("model_type", modelType)
                        .eq("is_default", 1)
                        .eq("is_deleted", 0)
        );
        for (ModelConfigEntity item : defaults) {
            if (!item.getId().equals(keepId)) {
                item.setIsDefault(0);
                item.setUpdatedAt(LocalDateTime.now());
                modelConfigMapper.update(item);
            }
        }
    }

    private ModelConfigEntity getConfigOrThrow(Long id) {
        ModelConfigEntity entity = modelConfigMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", id)
                        .eq("tenant_id", requireTenantId())
                        .eq("is_deleted", 0)
        );
        if (entity == null) {
            throw new BusinessException("模型配置不存在");
        }
        return entity;
    }

    private ModelConfigVO toVO(ModelConfigEntity entity) {
        ModelProviderEntity provider = modelProviderService.getProviderOrThrow(entity.getProviderId());
        return ModelConfigVO.builder()
                .id(entity.getId())
                .providerId(entity.getProviderId())
                .providerCode(provider.getProviderCode())
                .providerName(provider.getProviderName())
                .modelName(entity.getModelName())
                .modelType(entity.getModelType())
                .displayName(entity.getDisplayName())
                .contextWindow(entity.getContextWindow())
                .maxOutputTokens(entity.getMaxOutputTokens())
                .inputPrice(entity.getInputPrice())
                .outputPrice(entity.getOutputPrice())
                .currency(BillingCurrency.fromProviderCode(provider.getProviderCode()).getCode())
                .defaultTemperature(entity.getDefaultTemperature())
                .enabled(entity.getIsEnabled() != null && entity.getIsEnabled() == 1)
                .isDefault(entity.getIsDefault() != null && entity.getIsDefault() == 1)
                .updatedAt(entity.getUpdatedAt())
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
