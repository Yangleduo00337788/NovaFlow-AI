package ai.novaflow.workflowengine.component;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent("noop")
public class WorkflowNoopNodeComponent extends NodeComponent {

    @Override
    public void process() {
        // 占位组件：用于 IF 分支为空时的兜底
    }
}
