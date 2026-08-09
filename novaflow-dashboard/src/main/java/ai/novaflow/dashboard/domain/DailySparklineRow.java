package ai.novaflow.dashboard.domain;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DailySparklineRow {

    private String dayLabel;
    private Long value;
    private LocalDate statDate;
}
