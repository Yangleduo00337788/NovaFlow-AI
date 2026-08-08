package ai.novaflow.knowledge.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class KnowledgeBaseVO {

    private Long id;
    private Long applicationId;
    private String kbName;
    private String description;
    private String embeddingModel;
    private String chunkStrategy;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private Integer documentCount;
    private Integer chunkCount;
    private Long totalSizeBytes;
    private String visibility;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
