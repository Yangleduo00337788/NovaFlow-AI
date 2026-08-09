package ai.novaflow.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Table("token_usage")
public class TokenUsageEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private Long applicationId;
    private Long agentId;
    private Long userId;
    private Long modelConfigId;
    private String usageType;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private BigDecimal cost;
    private String currency;
    private Integer latencyMs;
    private Boolean success;
    private LocalDate usageDate;
    private LocalDateTime createdAt;
}
