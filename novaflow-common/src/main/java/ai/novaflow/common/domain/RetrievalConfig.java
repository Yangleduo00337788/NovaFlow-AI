package ai.novaflow.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalConfig {

    public static final int DEFAULT_TOP_K = 5;
    public static final int DEFAULT_RERANK_CANDIDATE_MULTIPLIER = 3;

    private Integer topK;
    private Float scoreThreshold;
    private Boolean rerankEnabled;
    private String rerankModel;
    private Integer rerankCandidateK;

    public int effectiveTopK() {
        return topK != null && topK > 0 ? topK : DEFAULT_TOP_K;
    }

    public Float effectiveScoreThreshold() {
        return scoreThreshold;
    }

    public boolean effectiveRerankEnabled() {
        return Boolean.TRUE.equals(rerankEnabled);
    }

    public int effectiveRerankCandidateK(int topK) {
        if (rerankCandidateK != null && rerankCandidateK > topK) {
            return rerankCandidateK;
        }
        return Math.max(topK * DEFAULT_RERANK_CANDIDATE_MULTIPLIER, topK + 2);
    }
}
