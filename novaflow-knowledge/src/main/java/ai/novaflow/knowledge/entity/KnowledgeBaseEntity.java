package ai.novaflow.knowledge.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("knowledge_base")
public class KnowledgeBaseEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
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
    private String qdrantCollection;
    private String visibility;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
