package ai.novaflow.workflowengine.component;

import ai.novaflow.workflowengine.domain.WorkflowExecutionContext;
import ai.novaflow.workflowengine.domain.WorkflowNodeDefinition;
import ai.novaflow.workflowengine.domain.WorkflowNodeProcessResult;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeBooleanComponent;
import org.springframework.util.StringUtils;

@LiteflowComponent("condition")
public class WorkflowConditionNodeComponent extends NodeBooleanComponent {

    @Override
    public boolean processBoolean() throws Exception {
        WorkflowExecutionContext context = this.getContextBean(WorkflowExecutionContext.class);
        if (context.isFailed()) {
            return false;
        }
        String nodeId = this.getTag();
        WorkflowNodeDefinition node = context.requireNode(nodeId);
        long startedAt = System.currentTimeMillis();
        WorkflowNodeProcessResult result = context.getNodeProcessor().process(
                node,
                context.getPayload(),
                context
        );
        int durationMs = (int) (System.currentTimeMillis() - startedAt);
        context.recordStep(node, context.getPayload(), result, durationMs);
        if (!result.isSuccess()) {
            this.setIsEnd(true);
            return false;
        }
        return isTrue(result.getOutput());
    }

    private boolean isTrue(String output) {
        if (!StringUtils.hasText(output)) {
            return false;
        }
        return "true".equalsIgnoreCase(output.trim());
    }
}
