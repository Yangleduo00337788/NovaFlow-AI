package ai.novaflow.dashboard.domain;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardOverviewVO {

    private List<StatCardVO> stats;
    private List<RecentItemVO> recentItems;
    private List<RecentLogVO> recentLogs;
    private List<ModelUsageVO> modelUsage;
    private List<TopAppVO> topApps;
    private List<SystemHealthVO> systemHealth;
    private List<TrendPointVO> trend;
    private List<QuickActionVO> quickActions;
    private PlanInfoVO planInfo;

    @Data
    @Builder
    public static class StatCardVO {
        private String key;
        private String label;
        private String value;
        private String change;
        private boolean up;
    }

    @Data
    @Builder
    public static class RecentItemVO {
        private String name;
        private String type;
        private String updatedAt;
        private String path;
    }

    @Data
    @Builder
    public static class RecentLogVO {
        private String name;
        private String status;
        private boolean success;
        private String time;
        private String duration;
        private int tokens;
    }

    @Data
    @Builder
    public static class ModelUsageVO {
        private String model;
        private int percent;
        private String tokens;
    }

    @Data
    @Builder
    public static class TopAppVO {
        private String name;
        private String value;
    }

    @Data
    @Builder
    public static class SystemHealthVO {
        private String name;
        private String status;
        private boolean healthy;
    }

    @Data
    @Builder
    public static class TrendPointVO {
        private String time;
        private long value;
    }

    @Data
    @Builder
    public static class QuickActionVO {
        private String key;
        private String label;
        private String path;
    }

    @Data
    @Builder
    public static class PlanInfoVO {
        private String planType;
        private String expireAt;
        private int usedPercent;
    }
}
