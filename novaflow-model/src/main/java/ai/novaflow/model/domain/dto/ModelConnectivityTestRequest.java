package ai.novaflow.model.domain.dto;

import lombok.Data;

@Data
public class ModelConnectivityTestRequest {

    private String apiKey;
    private String baseUrl;
    private String modelName;
}
