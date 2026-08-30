package ai.novaflow.workflow.util;

import ai.novaflow.workflow.domain.WorkflowNodeType;
import ai.novaflow.workflow.entity.WorkflowEdgeEntity;
import ai.novaflow.workflow.entity.WorkflowNodeEntity;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

        String chainBody = buildChainFrom(start.getNodeId(), nodeMap, outgoing, new HashSet<>());
        if (!StringUtils.hasText(chainBody)) {
            return null;
        }
        return "THEN(" + chainBody + ")";
    }

    public static boolean hasConditionalBranching(List<WorkflowNodeEntity> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return false;
        }
        return nodes.stream().anyMatch(node -> WorkflowNodeType.CONDITION.equals(node.getNodeType()));
    }

    public static boolean usesLegacySyntax(String elExpression) {
        if (!StringUtils.hasText(elExpression)) {
            return false;
        }
        return !elExpression.contains(".tag(");
    }

    private static String buildChainFrom(
            String nodeId,
            Map<String, WorkflowNodeEntity> nodeMap,
            Map<String, List<WorkflowEdgeEntity>> outgoing,
            Set<String> visiting) {
        if (!StringUtils.hasText(nodeId) || visiting.contains(nodeId)) {
            return "";
        }
        visiting.add(nodeId);

        WorkflowNodeEntity node = nodeMap.get(nodeId);
        if (node == null) {
            visiting.remove(nodeId);
            return "";
        }

        String current = nodeExpression(node);
        if (WorkflowNodeType.END.equals(node.getNodeType())) {
            visiting.remove(nodeId);
            return current;
        }

        List<WorkflowEdgeEntity> nextEdges = outgoing.getOrDefault(nodeId, List.of());
        if (nextEdges.isEmpty()) {
            visiting.remove(nodeId);
            return current;
        }

        if (WorkflowNodeType.CONDITION.equals(node.getNodeType())) {
            String trueTarget = resolveBranchTarget(nextEdges, "true");
            String falseTarget = resolveBranchTarget(nextEdges, "false");
            String trueBranch = wrapBranch(buildChainFrom(trueTarget, nodeMap, outgoing, new HashSet<>(visiting)));
            String falseBranch = wrapBranch(buildChainFrom(falseTarget, nodeMap, outgoing, new HashSet<>(visiting)));
            visiting.remove(nodeId);
            return "IF(" + current + ", " + trueBranch + ", " + falseBranch + ")";
        }

        String tail = buildChainFrom(nextEdges.get(0).getTargetNodeId(), nodeMap, outgoing, visiting);
        visiting.remove(nodeId);
        if (!StringUtils.hasText(tail)) {
            return current;
        }
        return current + ", " + tail;
    }

    private static String wrapBranch(String inner) {
        if (!StringUtils.hasText(inner)) {
            return "THEN(noop)";
        }
        if (inner.startsWith("IF(") || inner.startsWith("THEN(")) {
            return inner;
        }
        return "THEN(" + inner + ")";
    }

    private static String resolveBranchTarget(List<WorkflowEdgeEntity> edges, String branch) {
        String normalizedBranch = branch.toLowerCase(Locale.ROOT);
        for (WorkflowEdgeEntity edge : edges) {
            String handle = normalize(edge.getSourceHandle());
            String condition = normalize(edge.getCondition());
            if (normalizedBranch.equals(handle) || normalizedBranch.equals(condition)) {
                return edge.getTargetNodeId();
            }
        }
        if ("true".equals(normalizedBranch) && !edges.isEmpty()) {
            return edges.get(0).getTargetNodeId();
        }
        if ("false".equals(normalizedBranch) && edges.size() > 1) {
            return edges.get(1).getTargetNodeId();
        }
        return null;
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
    }

    private static String nodeExpression(WorkflowNodeEntity node) {
        return node.getNodeType() + ".tag(\"" + escapeTag(node.getNodeId()) + "\")";
    }

    private static String escapeTag(String nodeId) {
        return nodeId == null ? "" : nodeId.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
