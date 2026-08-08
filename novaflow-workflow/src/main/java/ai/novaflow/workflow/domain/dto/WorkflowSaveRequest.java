package ai.novaflow.workflow.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class WorkflowSaveRequest {

    @NotBlank(message = "工作流名称不能为空")
    private String workflowName;

    private String description;

    @NotNull(message = "所属应用不能为空")
    private Long applicationId;

    private WorkflowCanvasData canvasData;

    @Data
    public static class WorkflowCanvasData {
        private List<WorkflowCanvasNode> nodes;
        private List<WorkflowCanvasEdge> edges;
        private Map<String, Object> viewport;
    }

    @Data
    public static class WorkflowCanvasNode {
        private String id;
        private String type;
        private WorkflowCanvasPosition position;
        private WorkflowCanvasNodeData data;
    }

    @Data
    public static class WorkflowCanvasPosition {
        private Double x;
        private Double y;
    }

    @Data
    public static class WorkflowCanvasNodeData {
        private String label;
        private Map<String, Object> config;
    }

    @Data
    public static class WorkflowCanvasEdge {
        private String id;
        private String source;
        private String target;
        private String sourceHandle;
        private String targetHandle;
        private String label;
    }
}
