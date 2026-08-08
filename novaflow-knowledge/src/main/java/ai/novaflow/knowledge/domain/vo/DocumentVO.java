package ai.novaflow.knowledge.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentVO {

    private Long id;
    private Long knowledgeBaseId;
    private String docName;
    private String docType;
    private Long fileSize;
    private String fileHash;
    private String sourceType;
    private Integer processStatus;
    private String processStatusLabel;
    private String processError;
    private Integer chunkCount;
    private Integer charCount;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
