package ai.novaflow.workflow.service;

import ai.novaflow.workflow.domain.WorkflowNodeType;
import ai.novaflow.workflow.domain.WorkflowStatus;
import ai.novaflow.workflow.entity.WorkflowEdgeEntity;
import ai.novaflow.workflow.entity.WorkflowEntity;
import ai.novaflow.workflow.entity.WorkflowNodeEntity;
import ai.novaflow.workflow.mapper.WorkflowEdgeMapper;
import ai.novaflow.workflow.mapper.WorkflowMapper;
import ai.novaflow.workflow.mapper.WorkflowNodeMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowElRefreshServiceTest {

    @Mock
    private WorkflowMapper workflowMapper;

    @Mock
    private WorkflowNodeMapper workflowNodeMapper;

    @Mock
    private WorkflowEdgeMapper workflowEdgeMapper;

    @InjectMocks
    private WorkflowElRefreshService workflowElRefreshService;

    @Test
    void refreshPublishedWorkflowElUpdatesLegacyExpression() {
        WorkflowEntity workflow = new WorkflowEntity();
        workflow.setId(1L);
        workflow.setTenantId(10L);
        workflow.setStatus(WorkflowStatus.PUBLISHED);
        workflow.setElExpression("THEN(start:n1, end:n2)");

        WorkflowNodeEntity start = node("n1", WorkflowNodeType.START);
        WorkflowNodeEntity end = node("n2", WorkflowNodeType.END);
        WorkflowEdgeEntity edge = new WorkflowEdgeEntity();
        edge.setSourceNodeId("n1");
        edge.setTargetNodeId("n2");

        when(workflowMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(List.of(workflow));
        when(workflowNodeMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(List.of(start, end));
        when(workflowEdgeMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(List.of(edge));

        int refreshed = workflowElRefreshService.refreshPublishedWorkflowEl();

        assertEquals(1, refreshed);
        ArgumentCaptor<WorkflowEntity> captor = ArgumentCaptor.forClass(WorkflowEntity.class);
        verify(workflowMapper).update(captor.capture());
        assertTrue(captor.getValue().getElExpression().contains(".tag("));
    }

    private WorkflowNodeEntity node(String nodeId, String type) {
        WorkflowNodeEntity node = new WorkflowNodeEntity();
        node.setNodeId(nodeId);
        node.setNodeType(type);
        return node;
    }
}
