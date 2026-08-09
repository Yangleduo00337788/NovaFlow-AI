package ai.novaflow.model.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TokenUsageLogRow {

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
    private Integer latencyMs;
    private Integer success;
    private Long userId;
    private LocalDateTime createdAt;
}
