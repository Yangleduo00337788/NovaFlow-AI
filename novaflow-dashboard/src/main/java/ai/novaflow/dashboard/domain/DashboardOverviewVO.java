package ai.novaflow.dashboard.domain;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardOverviewVO {

    private List<StatCardVO> stats;
    private List<RecentItemVO> recentItems;
    private List<RecentItemVO> favoriteItems;
    private List<RecentLogVO> recentLogs;
    private List<ModelUsageVO> modelUsage;
    private List<TopAppVO> topApps;
    private WorkflowRuntimeVO workflowRuntime;
    private List<SystemHealthVO> systemHealth;
    private List<TrendPointVO> trend;
    private List<QuickActionVO> quickActions;
    private List<QuickStartTileVO> quickStartTiles;
    private PlanInfoVO planInfo;
    private String totalModelTokens;
    private Map<String, List<Long>> sparklines;

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
        private String resourceType;
        private Long resourceId;
        private boolean favorite;
    }

    @Data
    @Builder
    public static class RecentLogVO {
        private Long logId;
        private String traceId;
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
    public static class QuickStartTileVO {
        private String key;
        private String label;
        private String desc;
        private String path;
        private String color;
    }

    @Data
    @Builder
    public static class PlanInfoVO {
        private String planType;
        private String expireAt;
        private int usedPercent;
    }

    @Data
    @Builder
    public static class WorkflowRuntimeVO {
        private Long workflowId;
        private String workflowName;
        private String executionId;
        private int status;
        private String statusLabel;
        private boolean running;
        private String path;
        private List<WorkflowRuntimeNodeVO> nodes;
        private WorkflowCanvasVO canvas;
    }

    @Data
    @Builder
    public static class WorkflowCanvasVO {
        private List<WorkflowCanvasNodeVO> nodes;
        private List<WorkflowCanvasEdgeVO> edges;
    }

    @Data
    @Builder
    public static class WorkflowCanvasNodeVO {
        private String id;
        private String type;
        private CanvasPositionVO position;
        private CanvasNodeDataVO data;
        private int status;
        private String statusLabel;
    }

    @Data
    @Builder
    public static class CanvasPositionVO {
        private double x;
        private double y;
    }

    @Data
    @Builder
    public static class CanvasNodeDataVO {
        private String label;
    }

    @Data
    @Builder
    public static class WorkflowCanvasEdgeVO {
        private String id;
        private String source;
        private String target;
        private String sourceHandle;
        private String targetHandle;
        private String label;
    }

    @Data
    @Builder
    public static class WorkflowRuntimeNodeVO {
        private String nodeId;
        private String nodeName;
        private String nodeType;
        private int status;
        private String statusLabel;
    }

    @Data
    @Builder
    public static class PublishedWorkflowVO {
        private Long workflowId;
        private String workflowName;
        private String applicationName;
        private int status;
        private String statusLabel;
        private String path;
        private String updatedAt;
    }
}
