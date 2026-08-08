package ai.novaflow.knowledge.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("document")
public class DocumentEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private Long knowledgeBaseId;
    private String docName;
    private String docType;
    private String filePath;
    private Long fileSize;
    private String fileHash;
    private String sourceType;
    private String sourceUrl;
    private Integer processStatus;
    private String processError;
    private Integer chunkCount;
    private Integer charCount;
    private LocalDateTime processedAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}
