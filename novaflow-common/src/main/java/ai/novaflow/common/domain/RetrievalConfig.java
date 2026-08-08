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

    private Integer topK;
    private Float scoreThreshold;

    public int effectiveTopK() {
        return topK != null && topK > 0 ? topK : DEFAULT_TOP_K;
    }

    public Float effectiveScoreThreshold() {
        return scoreThreshold;
    }
}
