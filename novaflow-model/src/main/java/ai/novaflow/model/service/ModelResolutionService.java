package ai.novaflow.model.service;

import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.model.domain.ModelProviderPreset;
import ai.novaflow.model.domain.ResolvedModelConfig;
import ai.novaflow.model.entity.ModelConfigEntity;
import ai.novaflow.model.entity.ModelProviderEntity;
import ai.novaflow.model.mapper.ModelConfigMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelResolutionService {

    private final ModelConfigMapper modelConfigMapper;
    private final ModelProviderService modelProviderService;

    public ResolvedModelConfig resolve(Long modelConfigId, Long tenantId) {
        ModelConfigEntity config = modelConfigId != null
                ? getConfigOrThrow(modelConfigId, tenantId)
                : getDefaultChatModel(tenantId);
        return toResolvedConfig(config);
    }

    public ResolvedModelConfig resolve(Long modelConfigId, Long tenantId,
                                       BigDecimal temperatureOverride, Integer maxTokensOverride) {
        ResolvedModelConfig config = resolve(modelConfigId, tenantId);
        if (temperatureOverride != null) {
            config.setTemperature(temperatureOverride);
        }
        if (maxTokensOverride != null) {
            config.setMaxTokens(maxTokensOverride);
        }
        return config;
    }

    private ResolvedModelConfig toResolvedConfig(ModelConfigEntity config) {
        if (config.getIsEnabled() == null || config.getIsEnabled() != 1) {
            throw new BusinessException("模型已停用: " + config.getModelName());
        }
        ModelProviderEntity provider = modelProviderService.getProviderOrThrow(config.getProviderId());
        if (provider.getIsEnabled() == null || provider.getIsEnabled() != 1) {
            throw new BusinessException("模型提供商已停用");
        }

        ModelProviderPreset preset = ModelProviderPreset.of(provider.getProviderCode())
                .orElseThrow(() -> new BusinessException("不支持的模型提供商: " + provider.getProviderCode()));

        String apiKey = modelProviderService.resolveApiKey(provider, null);
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException("模型提供商 API Key 未配置");
        }

        String baseUrl = StringUtils.hasText(provider.getBaseUrl())
                ? provider.getBaseUrl()
                : preset.getDefaultBaseUrl();

        return ResolvedModelConfig.builder()
                .modelConfigId(config.getId())
                .providerCode(provider.getProviderCode())
                .providerName(provider.getProviderName())
                .modelName(config.getModelName())
                .displayName(config.getDisplayName())
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .temperature(config.getDefaultTemperature() != null
                        ? config.getDefaultTemperature()
                        : new BigDecimal("0.70"))
                .maxTokens(config.getMaxOutputTokens() != null ? config.getMaxOutputTokens() : 2048)
                .build();
    }

    private ModelConfigEntity getConfigOrThrow(Long id, Long tenantId) {
        ModelConfigEntity entity = modelConfigMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", id)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
        );
        if (entity == null) {
            throw new BusinessException("模型配置不存在");
        }
        return entity;
    }

    private ModelConfigEntity getDefaultChatModel(Long tenantId) {
        List<ModelConfigEntity> defaults = modelConfigMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("model_type", "chat")
                        .eq("is_enabled", 1)
                        .eq("is_deleted", 0)
                        .orderBy("is_default", false)
                        .orderBy("updated_at", false)
        );
        if (defaults.isEmpty()) {
            throw new BusinessException("未配置可用的 Chat 模型，请先在模型中心配置并同步");
        }
        return defaults.get(0);
    }
}
