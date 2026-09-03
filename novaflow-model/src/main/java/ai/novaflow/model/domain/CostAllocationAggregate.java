package ai.novaflow.model.domain;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CostAllocationAggregate {

    private Long dimensionId;
    private String dimensionName;
    private Long calls;
    private Long tokens;
    private BigDecimal costCny;
    private BigDecimal costUsd;
}
