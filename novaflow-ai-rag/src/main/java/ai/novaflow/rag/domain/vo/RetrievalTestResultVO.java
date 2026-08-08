package ai.novaflow.rag.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RetrievalTestResultVO {

    private String query;
    private Integer topK;
    private Long latencyMs;
    private List<RetrievedChunkVO> chunks;
}
