package ai.novaflow.model.service;

import ai.novaflow.model.domain.UpstreamModelDescriptor;
import ai.novaflow.model.domain.vo.ModelConnectivityTestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelConnectivityService {

    private final ModelUpstreamService modelUpstreamService;

    public ModelConnectivityTestVO test(String baseUrl, String apiKey, String modelName, boolean requiresApiKey) {
        long start = System.currentTimeMillis();
        List<UpstreamModelDescriptor> models = modelUpstreamService.listModels(baseUrl, apiKey, requiresApiKey);
        String resolvedModel = StringUtils.hasText(modelName)
                ? modelName
                : models.stream().findFirst().map(UpstreamModelDescriptor::getModelName).orElse(null);

        return ModelConnectivityTestVO.builder()
                .success(true)
                .message("连接成功，获取到 " + models.size() + " 个上游模型")
                .latencyMs(System.currentTimeMillis() - start)
                .modelName(resolvedModel)
                .build();
    }
}
