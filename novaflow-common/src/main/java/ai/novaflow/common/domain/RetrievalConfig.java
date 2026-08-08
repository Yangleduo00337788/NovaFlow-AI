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
    private Boolean hybridEnabled;
    private Float hybridAlpha;

    public static final float DEFAULT_HYBRID_ALPHA = 0.7F;

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

    public boolean effectiveHybridEnabled() {
        return Boolean.TRUE.equals(hybridEnabled);
    }

    public float effectiveHybridAlpha() {
        if (hybridAlpha == null) {
            return DEFAULT_HYBRID_ALPHA;
        }
        return Math.max(0F, Math.min(1F, hybridAlpha));
    }

    public int effectiveHybridCandidateK(int topK) {
        return Math.max(effectiveRerankCandidateK(topK), topK * 5);
    }
}
