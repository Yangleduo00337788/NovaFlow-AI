package ai.novaflow.dashboard.domain;

import lombok.Data;

@Data
public class DailySparklineRow {

    private String dayLabel;
    private Long value;
}
