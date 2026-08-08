package ai.novaflow.dashboard.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecentUsageLogRow {

    private String agentName;
    private Integer totalTokens;
    private Integer latencyMs;
    private LocalDateTime createdAt;
}
