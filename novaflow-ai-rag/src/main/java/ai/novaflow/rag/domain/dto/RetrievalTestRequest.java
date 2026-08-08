package ai.novaflow.rag.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RetrievalTestRequest {

    @NotBlank(message = "检索问题不能为空")
    @Size(max = 2000, message = "检索问题不能超过2000个字符")
    private String query;

    @Min(value = 1, message = "Top-K 不能小于1")
    @Max(value = 20, message = "Top-K 不能大于20")
    private Integer topK = 5;

    @Min(value = 0, message = "相似度阈值不能小于0")
    @Max(value = 1, message = "相似度阈值不能大于1")
    private Float scoreThreshold;

    private Boolean rerankEnabled;
    private String rerankModel;
    private Integer rerankCandidateK;
    private Boolean hybridEnabled;

    @Min(value = 0, message = "混合检索向量权重不能小于0")
    @Max(value = 1, message = "混合检索向量权重不能大于1")
    private Float hybridAlpha;
}
