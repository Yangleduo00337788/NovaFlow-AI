package ai.novaflow.knowledge.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeBaseSaveRequest {

    private Long applicationId;

    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 128, message = "知识库名称不能超过128个字符")
    private String kbName;

    @Size(max = 512, message = "描述不能超过512个字符")
    private String description;

    @NotBlank(message = "Embedding 模型不能为空")
    private String embeddingModel;

    private String chunkStrategy = "fixed";

    @Min(value = 128, message = "分块大小不能小于128")
    @Max(value = 4096, message = "分块大小不能大于4096")
    private Integer chunkSize = 512;

    @Min(value = 0, message = "分块重叠不能小于0")
    @Max(value = 512, message = "分块重叠不能大于512")
    private Integer chunkOverlap = 50;

    @Min(value = 1, message = "Top-K 不能小于1")
    @Max(value = 20, message = "Top-K 不能大于20")
    private Integer retrievalTopK = 5;

    @Min(value = 0, message = "相似度阈值不能小于0")
    @Max(value = 1, message = "相似度阈值不能大于1")
    private Float retrievalScoreThreshold;

    private String visibility = "private";
}
