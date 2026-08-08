package ai.novaflow.monitor.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MonitorOverviewVO {

    private List<MetricCardVO> metrics;
    private List<ServiceHealthVO> services;
    private List<RankingItemVO> topAgents;
    private List<RankingItemVO> topApplications;
    private List<TrendPointVO> hourlyTrend;

    @Data
    @Builder
    public static class MetricCardVO {
        private String key;
        private String label;
        private String value;
        private String hint;
    }

    @Data
    @Builder
    public static class ServiceHealthVO {
        private String key;
        private String name;
        private boolean healthy;
        private String status;
        private String detail;
    }

    @Data
    @Builder
    public static class RankingItemVO {
        private String name;
        private Long value;
        private String valueLabel;
    }

    @Data
    @Builder
    public static class TrendPointVO {
        private String time;
        private Long value;
    }
}
