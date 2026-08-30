package ai.novaflow.model.controller;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.model.domain.dto.ModelConfigSaveRequest;
import ai.novaflow.model.domain.dto.ModelConnectivityTestRequest;
import ai.novaflow.model.domain.dto.ModelProviderSaveRequest;
import ai.novaflow.model.domain.vo.EmbeddingOptionVO;
import ai.novaflow.model.domain.vo.ModelConfigVO;
import ai.novaflow.model.domain.vo.ModelConnectivityTestVO;
import ai.novaflow.model.domain.vo.ModelOverviewVO;
import ai.novaflow.model.domain.vo.ModelProviderVO;
import ai.novaflow.model.domain.vo.ModelSyncResultVO;
import ai.novaflow.model.service.ModelConfigService;
import ai.novaflow.model.service.ModelProviderService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@SaCheckPermission("model:config")
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelProviderService modelProviderService;
    private final ModelConfigService modelConfigService;

    @GetMapping("/overview")
    public ApiResult<ModelOverviewVO> overview() {
        return ApiResult.ok(modelProviderService.overview());
    }

    @GetMapping("/providers")
    public ApiResult<List<ModelProviderVO>> listProviders() {
        return ApiResult.ok(modelProviderService.listProviders());
    }

    @GetMapping("/providers/{id}")
    public ApiResult<ModelProviderVO> providerDetail(@PathVariable Long id) {
        return ApiResult.ok(modelProviderService.detail(id));
    }

    @PostMapping("/providers")
    public ApiResult<ModelProviderVO> saveProvider(@Valid @RequestBody ModelProviderSaveRequest request) {
        return ApiResult.ok(modelProviderService.save(request));
    }

    @PutMapping("/providers/{id}")
    public ApiResult<ModelProviderVO> updateProvider(
            @PathVariable Long id,
            @Valid @RequestBody ModelProviderSaveRequest request) {
        ModelProviderVO existing = modelProviderService.detail(id);
        request.setProviderCode(existing.getProviderCode());
        return ApiResult.ok(modelProviderService.save(request));
    }

    @DeleteMapping("/providers/{id}")
    public ApiResult<Void> deleteProvider(@PathVariable Long id) {
        modelProviderService.delete(id);
        return ApiResult.ok();
    }

    @PostMapping("/providers/{id}/test")
    public ApiResult<ModelConnectivityTestVO> testProvider(
            @PathVariable Long id,
            @RequestBody(required = false) ModelConnectivityTestRequest request) {
        return ApiResult.ok(modelProviderService.test(id, request));
    }

    @PostMapping("/providers/{id}/sync")
    public ApiResult<ModelSyncResultVO> syncProviderModels(@PathVariable Long id) {
        return ApiResult.ok(modelProviderService.syncModels(id));
    }

    @GetMapping("/embedding-options")
    public ApiResult<List<EmbeddingOptionVO>> listEmbeddingOptions() {
        return ApiResult.ok(modelConfigService.listEmbeddingOptions());
    }

    @GetMapping("/configs")
    public ApiResult<List<ModelConfigVO>> listConfigs(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) String modelType) {
        return ApiResult.ok(modelConfigService.list(providerId, modelType));
    }

    @GetMapping("/configs/{id}")
    public ApiResult<ModelConfigVO> configDetail(@PathVariable Long id) {
        return ApiResult.ok(modelConfigService.detail(id));
    }

    @PostMapping("/configs")
    public ApiResult<ModelConfigVO> createConfig(@Valid @RequestBody ModelConfigSaveRequest request) {
        return ApiResult.ok(modelConfigService.create(request));
    }

    @PutMapping("/configs/{id}")
    public ApiResult<ModelConfigVO> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody ModelConfigSaveRequest request) {
        return ApiResult.ok(modelConfigService.update(id, request));
    }

    @DeleteMapping("/configs/{id}")
    public ApiResult<Void> deleteConfig(@PathVariable Long id) {
        modelConfigService.delete(id);
        return ApiResult.ok();
    }

    @PutMapping("/configs/{id}/default")
    public ApiResult<ModelConfigVO> setDefaultConfig(@PathVariable Long id) {
        return ApiResult.ok(modelConfigService.setDefault(id));
    }
}
