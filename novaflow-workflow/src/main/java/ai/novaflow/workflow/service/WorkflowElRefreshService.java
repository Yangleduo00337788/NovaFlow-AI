package ai.novaflow.workflow.service;

import ai.novaflow.workflow.domain.WorkflowStatus;
import ai.novaflow.workflow.entity.WorkflowEdgeEntity;
import ai.novaflow.workflow.entity.WorkflowEntity;
import ai.novaflow.workflow.entity.WorkflowNodeEntity;
import ai.novaflow.workflow.mapper.WorkflowEdgeMapper;
import ai.novaflow.workflow.mapper.WorkflowMapper;
import ai.novaflow.workflow.mapper.WorkflowNodeMapper;
import ai.novaflow.workflow.util.WorkflowElBuilder;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowElRefreshService {

    private final WorkflowMapper workflowMapper;
    private final WorkflowNodeMapper workflowNodeMapper;
    private final WorkflowEdgeMapper workflowEdgeMapper;

    @Transactional
    public int refreshPublishedWorkflowEl() {
        List<WorkflowEntity> workflows = workflowMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("status", WorkflowStatus.PUBLISHED)
                        .eq("is_deleted", 0)
        );
        int refreshed = 0;
        for (WorkflowEntity workflow : workflows) {
            if (refreshWorkflowEl(workflow)) {
                refreshed++;
            }
        }
        if (refreshed > 0) {
            log.info("Refreshed LiteFlow EL for {} published workflow(s)", refreshed);
        }
        return refreshed;
    }

    private boolean refreshWorkflowEl(WorkflowEntity workflow) {
        List<WorkflowNodeEntity> nodes = workflowNodeMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("workflow_id", workflow.getId())
                        .eq("tenant_id", workflow.getTenantId())
                        .orderBy("sort_order", true)
        );
        if (nodes.isEmpty()) {
            return false;
        }
        List<WorkflowEdgeEntity> edges = workflowEdgeMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("workflow_id", workflow.getId())
                        .eq("tenant_id", workflow.getTenantId())
        );
        String newEl = WorkflowElBuilder.build(nodes, edges);
        if (!StringUtils.hasText(newEl)) {
            return false;
        }
        if (newEl.equals(workflow.getElExpression())) {
            return false;
        }
        workflow.setElExpression(newEl);
        workflow.setUpdatedAt(LocalDateTime.now());
        workflowMapper.update(workflow);
        return true;
    }
}
