package ai.novaflow.model.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TokenUsageLogVO {

    private Long id;
    private Long agentId;
    private String agentName;
    private String modelName;
    private String displayName;
    private String usageType;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private BigDecimal cost;
    private String currency;
    private String costLabel;
    private Integer latencyMs;
    private Long userId;
    private LocalDateTime createdAt;
}
