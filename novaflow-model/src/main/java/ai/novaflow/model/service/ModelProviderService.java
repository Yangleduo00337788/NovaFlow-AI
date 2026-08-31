package ai.novaflow.model.service;

import ai.novaflow.common.audit.AuditRecorder;
import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.util.CryptoService;
import ai.novaflow.model.domain.ModelProviderPreset;
import ai.novaflow.model.domain.dto.ModelConnectivityTestRequest;
import ai.novaflow.model.domain.dto.ModelProviderSaveRequest;
import ai.novaflow.model.domain.vo.ModelConnectivityTestVO;
import ai.novaflow.model.domain.vo.ModelOverviewVO;
import ai.novaflow.model.domain.vo.ModelProviderVO;
import ai.novaflow.model.domain.vo.ModelSyncResultVO;
import ai.novaflow.model.domain.vo.ModelCostSummaryVO;
import ai.novaflow.model.domain.vo.ModelUsageStatsVO;
import ai.novaflow.model.entity.ModelConfigEntity;
import ai.novaflow.model.entity.ModelProviderEntity;
import ai.novaflow.model.mapper.ModelConfigMapper;
import ai.novaflow.model.mapper.ModelProviderMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModelProviderService {

    private final ModelProviderMapper modelProviderMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final CryptoService cryptoService;
    private final ModelConnectivityService modelConnectivityService;
    private final ModelSyncService modelSyncService;
    private final ModelUsageService modelUsageService;
    private final AuditRecorder auditRecorder;

    public List<ModelProviderVO> listProviders() {
        Long tenantId = requireTenantId();
        Map<String, ModelProviderEntity> configuredMap = modelProviderMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
        ).stream().collect(Collectors.toMap(ModelProviderEntity::getProviderCode, Function.identity(), (a, b) -> a));

        return Arrays.stream(ModelProviderPreset.values())
                .map(preset -> toProviderVO(preset, configuredMap.get(preset.getCode()), tenantId))
                .toList();
    }

    public List<ModelProviderVO> listProviderPresets() {
        return ModelProviderPreset.all().stream()
                .map(preset -> toProviderVO(preset, null, requireTenantId()))
                .toList();
    }

    public ModelProviderVO detail(Long id) {
        ModelProviderEntity entity = getProviderOrThrow(id);
        ModelProviderPreset preset = ModelProviderPreset.of(entity.getProviderCode())
                .orElseThrow(() -> new BusinessException("不支持的模型提供商"));
        return toProviderVO(preset, entity, entity.getTenantId());
    }

    @Transactional
    public ModelProviderVO save(ModelProviderSaveRequest request) {
        ModelProviderPreset preset = ModelProviderPreset.of(request.getProviderCode())
                .orElseThrow(() -> new BusinessException("不支持的模型提供商"));

        Long tenantId = requireTenantId();
        ModelProviderEntity existing = modelProviderMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("provider_code", preset.getCode())
                        .eq("is_deleted", 0)
        );

        if (existing == null) {
            validateSaveRequest(preset, request);
            String apiKey = resolveSaveApiKey(preset, request.getApiKey());
            ModelProviderEntity created = new ModelProviderEntity();
            created.setTenantId(tenantId);
            created.setProviderCode(preset.getCode());
            created.setProviderName(preset.getName());
            created.setBaseUrl(resolveBaseUrl(request.getBaseUrl(), preset.getDefaultBaseUrl()));
            if (StringUtils.hasText(apiKey)) {
                created.setApiKeyEncrypted(cryptoService.encrypt(apiKey));
            }
            created.setIsEnabled(Boolean.FALSE.equals(request.getEnabled()) ? 0 : 1);
            created.setIsDeleted(0);
            created.setCreatedAt(LocalDateTime.now());
            created.setUpdatedAt(LocalDateTime.now());
            modelProviderMapper.insert(created);
            modelSyncService.syncFromUpstream(created, apiKey);
            return toProviderVO(preset, created, tenantId);
        }

        existing.setBaseUrl(resolveBaseUrl(request.getBaseUrl(), existing.getBaseUrl()));
        if (StringUtils.hasText(request.getApiKey()) && !cryptoService.isMaskedValue(request.getApiKey())) {
            existing.setApiKeyEncrypted(cryptoService.encrypt(request.getApiKey().trim()));
        }
        if (request.getEnabled() != null) {
            existing.setIsEnabled(Boolean.TRUE.equals(request.getEnabled()) ? 1 : 0);
        }
        existing.setUpdatedAt(LocalDateTime.now());
        modelProviderMapper.update(existing);

        boolean apiKeyChanged = StringUtils.hasText(request.getApiKey()) && !cryptoService.isMaskedValue(request.getApiKey());
        if (apiKeyChanged) {
            modelSyncService.syncFromUpstream(existing, request.getApiKey().trim());
        }
        return toProviderVO(preset, existing, tenantId);
    }

    @Transactional
    public void delete(Long id) {
        ModelProviderEntity entity = getProviderOrThrow(id);
        entity.setIsDeleted(1);
        entity.setUpdatedAt(LocalDateTime.now());
        modelProviderMapper.update(entity);

        List<ModelConfigEntity> configs = modelConfigMapper.selectListByQuery(
                QueryWrapper.create().eq("provider_id", id).eq("is_deleted", 0)
        );
        for (ModelConfigEntity config : configs) {
            config.setIsDeleted(1);
            config.setUpdatedAt(LocalDateTime.now());
            modelConfigMapper.update(config);
        }
        auditRecorder.record("model_provider.delete", "model_provider", entity.getId(),
                "删除模型提供商: " + entity.getProviderCode());
    }

    public ModelConnectivityTestVO test(Long id, ModelConnectivityTestRequest request) {
        ModelProviderEntity entity = getProviderOrThrow(id);
        String apiKey = resolveApiKey(entity, request != null ? request.getApiKey() : null);
        String baseUrl = resolveBaseUrl(
                request != null ? request.getBaseUrl() : null,
                entity.getBaseUrl()
        );
        String modelName = request != null ? request.getModelName() : null;
        ModelProviderPreset preset = ModelProviderPreset.of(entity.getProviderCode()).orElse(null);
        boolean requiresApiKey = preset == null || preset.isRequiresApiKey();
        return modelConnectivityService.test(baseUrl, apiKey, modelName, requiresApiKey);
    }

    public ModelSyncResultVO syncModels(Long id) {
        ModelProviderEntity entity = getProviderOrThrow(id);
        return modelSyncService.syncFromUpstream(entity, resolveApiKey(entity, null));
    }

    public ModelOverviewVO overview() {
        Long tenantId = requireTenantId();
        long configuredProviders = modelProviderMapper.selectCountByQuery(
                QueryWrapper.create().eq("tenant_id", tenantId).eq("is_deleted", 0)
        );
        long enabledModels = modelConfigMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
                        .eq("is_enabled", 1)
        );
        ModelUsageStatsVO usageStats = modelUsageService.getUsageStats(tenantId);

        return ModelOverviewVO.builder()
                .totalCalls(usageStats.getTotalCalls())
                .totalTokens(usageStats.getTotalTokens())
                .totalCost(usageStats.getTotalCost())
                .costSummaries(usageStats.getCostSummaries())
                .configuredProviders(configuredProviders)
                .enabledModels(enabledModels)
                .topModels(usageStats.getTopModels())
                .build();
    }

    ModelProviderEntity getProviderOrThrow(Long id) {
        ModelProviderEntity entity = modelProviderMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", id)
                        .eq("tenant_id", requireTenantId())
                        .eq("is_deleted", 0)
        );
        if (entity == null) {
            throw new BusinessException("模型提供商不存在");
        }
        return entity;
    }

    String resolveApiKey(ModelProviderEntity entity, String overrideKey) {
        if (StringUtils.hasText(overrideKey) && !cryptoService.isMaskedValue(overrideKey)) {
            return overrideKey.trim();
        }
        if (!StringUtils.hasText(entity.getApiKeyEncrypted())) {
            ModelProviderPreset preset = ModelProviderPreset.of(entity.getProviderCode()).orElse(null);
            if (preset != null && !preset.isRequiresApiKey()) {
                return "ollama";
            }
            return "";
        }
        return cryptoService.decrypt(entity.getApiKeyEncrypted());
    }

    private ModelProviderVO toProviderVO(ModelProviderPreset preset, ModelProviderEntity entity, Long tenantId) {
        int modelCount = 0;
        String apiKeyMasked = null;
        String baseUrl = preset.getDefaultBaseUrl();
        boolean configured = entity != null;
        boolean enabled = false;
        LocalDateTime updatedAt = null;
        Long id = null;

        if (entity != null) {
            id = entity.getId();
            baseUrl = StringUtils.hasText(entity.getBaseUrl()) ? entity.getBaseUrl() : preset.getDefaultBaseUrl();
            if (StringUtils.hasText(entity.getApiKeyEncrypted())) {
                apiKeyMasked = cryptoService.maskSecret(cryptoService.decrypt(entity.getApiKeyEncrypted()));
            } else if (!preset.isRequiresApiKey()) {
                apiKeyMasked = "无需配置";
            }
            enabled = entity.getIsEnabled() != null && entity.getIsEnabled() == 1;
            updatedAt = entity.getUpdatedAt();
            modelCount = (int) modelConfigMapper.selectCountByQuery(
                    QueryWrapper.create()
                            .eq("tenant_id", tenantId)
                            .eq("provider_id", entity.getId())
                            .eq("is_deleted", 0)
            );
        }

        return ModelProviderVO.builder()
                .id(id)
                .providerCode(preset.getCode())
                .providerName(preset.getName())
                .description(preset.getDescription())
                .baseUrl(baseUrl)
                .defaultBaseUrl(preset.getDefaultBaseUrl())
                .apiKeyMasked(apiKeyMasked)
                .configured(configured)
                .enabled(enabled)
                .modelCount(modelCount)
                .region(preset.getRegion().name().toLowerCase())
                .apiStyle(preset.getApiStyle().name().toLowerCase())
                .requiresApiKey(preset.isRequiresApiKey())
                .updatedAt(updatedAt)
                .build();
    }

    private void validateSaveRequest(ModelProviderPreset preset, ModelProviderSaveRequest request) {
        if ("custom".equals(preset.getCode()) && !StringUtils.hasText(request.getBaseUrl())) {
            throw new BusinessException("自定义提供商请填写 Base URL");
        }
        if (preset.isRequiresApiKey() && !StringUtils.hasText(request.getApiKey())) {
            throw new BusinessException("首次配置请填写 API Key");
        }
    }

    private String resolveSaveApiKey(ModelProviderPreset preset, String apiKey) {
        if (StringUtils.hasText(apiKey)) {
            return apiKey.trim();
        }
        if (!preset.isRequiresApiKey()) {
            return "";
        }
        return null;
    }

    private String resolveBaseUrl(String requestBaseUrl, String fallback) {
        if (StringUtils.hasText(requestBaseUrl)) {
            return requestBaseUrl.trim();
        }
        return fallback;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户上下文缺失");
        }
        return tenantId;
    }
}
