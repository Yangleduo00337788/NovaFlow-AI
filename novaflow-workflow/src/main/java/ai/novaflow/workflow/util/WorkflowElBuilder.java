package ai.novaflow.workflow.util;

import ai.novaflow.workflow.domain.WorkflowNodeType;
import ai.novaflow.workflow.entity.WorkflowEdgeEntity;
import ai.novaflow.workflow.entity.WorkflowNodeEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class WorkflowElBuilder {

    private WorkflowElBuilder() {
    }

    public static String build(List<WorkflowNodeEntity> nodes, List<WorkflowEdgeEntity> edges) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        Map<String, WorkflowNodeEntity> nodeMap = nodes.stream()
                .collect(Collectors.toMap(WorkflowNodeEntity::getNodeId, node -> node, (a, b) -> a));
        Map<String, List<WorkflowEdgeEntity>> outgoing = edges == null ? Map.of() : edges.stream()
                .collect(Collectors.groupingBy(WorkflowEdgeEntity::getSourceNodeId));

        WorkflowNodeEntity start = nodes.stream()
                .filter(node -> WorkflowNodeType.START.equals(node.getNodeType()))
                .findFirst()
                .orElse(null);
        if (start == null) {
            return null;
        }

        List<String> chain = new ArrayList<>();
        String currentId = start.getNodeId();
        int guard = 0;
        while (currentId != null && guard++ < 64) {
            WorkflowNodeEntity current = nodeMap.get(currentId);
            if (current == null) {
                break;
            }
            chain.add(current.getNodeType() + ":" + current.getNodeId());
            if (WorkflowNodeType.END.equals(current.getNodeType())) {
                break;
            }
            List<WorkflowEdgeEntity> nextEdges = outgoing.getOrDefault(currentId, List.of());
            currentId = nextEdges.isEmpty() ? null : nextEdges.get(0).getTargetNodeId();
        }
        return "THEN(" + String.join(", ", chain) + ")";
    }
}
