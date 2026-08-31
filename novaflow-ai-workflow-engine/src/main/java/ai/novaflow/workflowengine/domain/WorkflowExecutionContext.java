package ai.novaflow.workflowengine.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class WorkflowExecutionContext {

    private Long tenantId;
    private Long triggeredByUserId;
    private String executionId;
    private String payload;
    private Map<String, WorkflowNodeDefinition> nodeMap = new HashMap<>();
    private WorkflowNodeProcessor nodeProcessor;
    private final List<WorkflowStepSnapshot> steps = new ArrayList<>();
    private boolean failed;
    private String errorMessage;
    private int totalTokens;
    private final List<Object> modelUsages = new ArrayList<>();

    public WorkflowNodeDefinition requireNode(String nodeId) {
        WorkflowNodeDefinition node = nodeMap.get(nodeId);
        if (node == null) {
            throw new IllegalStateException("工作流节点不存在: " + nodeId);
        }
        return node;
    }

    public void recordStep(
            WorkflowNodeDefinition node,
            String input,
            WorkflowNodeProcessResult result,
            int durationMs) {
        steps.add(WorkflowStepSnapshot.builder()
                .nodeId(node.getNodeId())
                .nodeType(node.getNodeType())
                .nodeName(node.getNodeName())
                .status(result.isSuccess() ? 1 : 2)
                .input(input)
                .output(result.getOutput())
                .errorMessage(result.getErrorMessage())
                .durationMs(durationMs)
                .tokensUsed(result.getTokensUsed())
                .modelUsage(result.getModelUsage())
                .build());
        totalTokens += Math.max(result.getTokensUsed(), 0);
        if (result.getModelUsage() != null) {
            modelUsages.add(result.getModelUsage());
        }
        if (!result.isSuccess()) {
            failed = true;
            errorMessage = result.getErrorMessage();
        } else if (result.getOutput() != null) {
            payload = result.getOutput();
        }
    }
}
