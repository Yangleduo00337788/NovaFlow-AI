package ai.novaflow.workflow.util;

import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.workflow.domain.WorkflowNodeType;
import ai.novaflow.workflow.entity.WorkflowEdgeEntity;
import ai.novaflow.workflow.entity.WorkflowNodeEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkflowPublishValidatorTest {

    @Test
    void rejectsEmptyGraph() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> WorkflowPublishValidator.validateGraph(List.of(), List.of()));
        assertEquals("发布失败：请至少添加一个节点", ex.getMessage());
    }

    @Test
    void rejectsMissingStartNode() {
        WorkflowNodeEntity end = node("end-1", WorkflowNodeType.END);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> WorkflowPublishValidator.validateGraph(List.of(end), List.of(edge("a", "b"))));
        assertEquals("发布失败：工作流需要且仅需要一个开始节点", ex.getMessage());
    }

    @Test
    void rejectsMissingEdges() {
        WorkflowNodeEntity start = node("start-1", WorkflowNodeType.START);
        WorkflowNodeEntity end = node("end-1", WorkflowNodeType.END);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> WorkflowPublishValidator.validateGraph(List.of(start, end), List.of()));
        assertEquals("发布失败：请连接节点后再发布", ex.getMessage());
    }

    @Test
    void acceptsMinimalValidGraph() {
        WorkflowNodeEntity start = node("start-1", WorkflowNodeType.START);
        WorkflowNodeEntity end = node("end-1", WorkflowNodeType.END);
        assertDoesNotThrow(() -> WorkflowPublishValidator.validateGraph(
                List.of(start, end),
                List.of(edge("start-1", "end-1"))));
    }

    private WorkflowNodeEntity node(String nodeId, String type) {
        WorkflowNodeEntity node = new WorkflowNodeEntity();
        node.setNodeId(nodeId);
        node.setNodeType(type);
        node.setNodeName(nodeId);
        return node;
    }

    private WorkflowEdgeEntity edge(String source, String target) {
        WorkflowEdgeEntity edge = new WorkflowEdgeEntity();
        edge.setSourceNodeId(source);
        edge.setTargetNodeId(target);
        return edge;
    }
}
