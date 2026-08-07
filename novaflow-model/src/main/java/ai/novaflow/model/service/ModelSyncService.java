package ai.novaflow.model.service;

import ai.novaflow.model.domain.UpstreamModelDescriptor;
import ai.novaflow.model.domain.vo.ModelSyncResultVO;
import ai.novaflow.model.entity.ModelConfigEntity;
import ai.novaflow.model.entity.ModelProviderEntity;
import ai.novaflow.model.mapper.ModelConfigMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModelSyncService {

    private final ModelConfigMapper modelConfigMapper;
    private final ModelUpstreamService modelUpstreamService;

    @Transactional
    public ModelSyncResultVO syncFromUpstream(ModelProviderEntity provider, String apiKey) {
        List<UpstreamModelDescriptor> upstreamModels = modelUpstreamService.listModels(provider.getBaseUrl(), apiKey);
        List<ModelConfigEntity> existingModels = modelConfigMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", provider.getTenantId())
                        .eq("provider_id", provider.getId())
                        .eq("is_deleted", 0)
        );

        Map<String, ModelConfigEntity> existingByName = existingModels.stream()
                .collect(Collectors.toMap(ModelConfigEntity::getModelName, item -> item, (left, right) -> left));

        Set<String> upstreamNames = new HashSet<>();
        int added = 0;
        int updated = 0;

        for (UpstreamModelDescriptor upstream : upstreamModels) {
            upstreamNames.add(upstream.getModelName());
            ModelConfigEntity existing = existingByName.get(upstream.getModelName());
            if (existing == null) {
                modelConfigMapper.insert(buildNewConfig(provider, upstream));
                added++;
                continue;
            }

            boolean changed = false;
            if (!upstream.getModelType().equals(existing.getModelType())) {
                existing.setModelType(upstream.getModelType());
                changed = true;
            }
            if (existing.getIsEnabled() == null || existing.getIsEnabled() == 0) {
                existing.setIsEnabled(1);
                changed = true;
            }
            if (changed) {
                existing.setUpdatedAt(LocalDateTime.now());
                modelConfigMapper.update(existing);
                updated++;
            }
        }

        int disabled = 0;
        for (ModelConfigEntity existing : existingModels) {
            if (!upstreamNames.contains(existing.getModelName()) && existing.getIsEnabled() != null && existing.getIsEnabled() == 1) {
                existing.setIsEnabled(0);
                existing.setUpdatedAt(LocalDateTime.now());
                modelConfigMapper.update(existing);
                disabled++;
            }
        }

        ensureDefaultChatModel(provider.getTenantId(), provider.getId());

        return ModelSyncResultVO.builder()
                .added(added)
                .updated(updated)
                .disabled(disabled)
                .total(upstreamModels.size())
                .message(String.format("已同步 %d 个上游模型（新增 %d，更新 %d，停用 %d）",
                        upstreamModels.size(), added, updated, disabled))
                .build();
    }

    private ModelConfigEntity buildNewConfig(ModelProviderEntity provider, UpstreamModelDescriptor upstream) {
        ModelConfigEntity config = new ModelConfigEntity();
        config.setTenantId(provider.getTenantId());
        config.setProviderId(provider.getId());
        config.setModelName(upstream.getModelName());
        config.setModelType(upstream.getModelType());
        config.setDisplayName(upstream.getDisplayName());
        config.setContextWindow(defaultContextWindow(upstream.getModelType()));
        config.setMaxOutputTokens("embedding".equals(upstream.getModelType()) ? 0 : 4096);
        config.setDefaultTemperature(new BigDecimal("0.70"));
        config.setIsEnabled(1);
        config.setIsDefault(0);
        config.setIsDeleted(0);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        return config;
    }

    private int defaultContextWindow(String modelType) {
        return switch (modelType) {
            case "embedding" -> 8192;
            case "rerank" -> 8192;
            default -> 32768;
        };
    }

    private void ensureDefaultChatModel(Long tenantId, Long providerId) {
        long defaultCount = modelConfigMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("provider_id", providerId)
                        .eq("model_type", "chat")
                        .eq("is_default", 1)
                        .eq("is_deleted", 0)
                        .eq("is_enabled", 1)
        );
        if (defaultCount > 0) {
            return;
        }

        ModelConfigEntity firstChat = modelConfigMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("provider_id", providerId)
                        .eq("model_type", "chat")
                        .eq("is_enabled", 1)
                        .eq("is_deleted", 0)
                        .orderBy("id", true)
                        .limit(1)
        );
        if (firstChat != null) {
            firstChat.setIsDefault(1);
            firstChat.setUpdatedAt(LocalDateTime.now());
            modelConfigMapper.update(firstChat);
        }
    }
}
