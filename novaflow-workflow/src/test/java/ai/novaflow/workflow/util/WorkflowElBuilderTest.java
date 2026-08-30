package ai.novaflow.workflow.util;

import ai.novaflow.workflow.domain.WorkflowNodeType;
import ai.novaflow.workflow.entity.WorkflowEdgeEntity;
import ai.novaflow.workflow.entity.WorkflowNodeEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowElBuilderTest {

    @Test
    void buildsLiteFlowTagSyntax() {
        WorkflowNodeEntity start = node("n1", WorkflowNodeType.START);
        WorkflowNodeEntity llm = node("n2", WorkflowNodeType.LLM);
        WorkflowNodeEntity end = node("n3", WorkflowNodeType.END);
        List<WorkflowEdgeEntity> edges = List.of(
                edge("n1", "n2"),
                edge("n2", "n3")
        );

        String el = WorkflowElBuilder.build(List.of(start, llm, end), edges);

        assertEquals("THEN(start.tag(\"n1\"), llm.tag(\"n2\"), end.tag(\"n3\"))", el);
    }

    @Test
    void buildsIfExpressionForConditionBranch() {
        WorkflowNodeEntity start = node("s1", WorkflowNodeType.START);
        WorkflowNodeEntity condition = node("c1", WorkflowNodeType.CONDITION);
        WorkflowNodeEntity trueEnd = node("e1", WorkflowNodeType.END);
        WorkflowNodeEntity falseEnd = node("e2", WorkflowNodeType.END);
        List<WorkflowEdgeEntity> edges = List.of(
                edge("s1", "c1"),
                branchEdge("c1", "e1", "true"),
                branchEdge("c1", "e2", "false")
        );

        String el = WorkflowElBuilder.build(List.of(start, condition, trueEnd, falseEnd), edges);

        assertEquals(
                "THEN(start.tag(\"s1\"), IF(condition.tag(\"c1\"), THEN(end.tag(\"e1\")), THEN(end.tag(\"e2\"))))",
                el
        );
    }

    @Test
    void escapesQuotesInNodeId() {
        WorkflowNodeEntity start = node("node\"1", WorkflowNodeType.START);
        WorkflowNodeEntity end = node("n2", WorkflowNodeType.END);

        String el = WorkflowElBuilder.build(List.of(start, end), List.of(edge("node\"1", "n2")));

        assertEquals("THEN(start.tag(\"node\\\"1\"), end.tag(\"n2\"))", el);
    }

    @Test
    void returnsNullWhenStartMissing() {
        WorkflowNodeEntity llm = node("n1", WorkflowNodeType.LLM);
        assertNull(WorkflowElBuilder.build(List.of(llm), List.of()));
    }

    @Test
    void detectsConditionalBranching() {
        WorkflowNodeEntity condition = node("c1", WorkflowNodeType.CONDITION);
        assertTrue(WorkflowElBuilder.hasConditionalBranching(List.of(condition)));
        assertFalse(WorkflowElBuilder.hasConditionalBranching(List.of(node("n1", WorkflowNodeType.LLM))));
    }

    @Test
    void detectsLegacySyntax() {
        assertTrue(WorkflowElBuilder.usesLegacySyntax("THEN(start:n1, llm:n2)"));
        assertFalse(WorkflowElBuilder.usesLegacySyntax("THEN(start.tag(\"n1\"), llm.tag(\"n2\"))"));
    }

    private WorkflowNodeEntity node(String nodeId, String type) {
        WorkflowNodeEntity node = new WorkflowNodeEntity();
        node.setNodeId(nodeId);
        node.setNodeType(type);
        return node;
    }

    private WorkflowEdgeEntity edge(String source, String target) {
        WorkflowEdgeEntity edge = new WorkflowEdgeEntity();
        edge.setSourceNodeId(source);
        edge.setTargetNodeId(target);
        return edge;
    }

    private WorkflowEdgeEntity branchEdge(String source, String target, String handle) {
        WorkflowEdgeEntity edge = edge(source, target);
        edge.setSourceHandle(handle);
        edge.setCondition(handle);
        return edge;
    }
}
