package ai.novaflow.rag.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RetrievedChunkVO {

    private Long knowledgeBaseId;
    private String knowledgeBaseName;
    private Long documentId;
    private String docName;
    private Integer chunkIndex;
    private String text;
    private Float score;
}
