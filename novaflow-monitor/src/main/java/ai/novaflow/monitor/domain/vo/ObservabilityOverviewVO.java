package ai.novaflow.monitor.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ObservabilityOverviewVO {

    private List<MonitorOverviewVO.MetricCardVO> metrics;
    private List<MonitorOverviewVO.ServiceHealthVO> services;
    private List<MonitorOverviewVO.TrendPointVO> failedTrend;
    private List<MonitorOverviewVO.TrendPointVO> latencyTrend;
}
