package ai.novaflow.workflowengine.component;

import ai.novaflow.workflowengine.domain.WorkflowExecutionContext;
import ai.novaflow.workflowengine.domain.WorkflowNodeDefinition;
import ai.novaflow.workflowengine.domain.WorkflowNodeProcessResult;
import com.yomahub.liteflow.core.NodeComponent;

abstract class AbstractWorkflowNodeComponent extends NodeComponent {

    @Override
    public void process() {
        WorkflowExecutionContext context = this.getContextBean(WorkflowExecutionContext.class);
        if (context.isFailed()) {
            return;
        }
        String nodeId = this.getTag();
        WorkflowNodeDefinition node = context.requireNode(nodeId);
        long startedAt = System.currentTimeMillis();
        WorkflowNodeProcessResult result = context.getNodeProcessor().process(
                node,
                context.getPayload(),
                context.getTenantId()
        );
        int durationMs = (int) (System.currentTimeMillis() - startedAt);
        context.recordStep(node, context.getPayload(), result, durationMs);
        if (!result.isSuccess()) {
            this.setIsEnd(true);
        }
    }
}
