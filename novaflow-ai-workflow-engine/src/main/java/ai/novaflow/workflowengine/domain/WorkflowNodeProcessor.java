package ai.novaflow.workflowengine.domain;

public interface WorkflowNodeProcessor {

    WorkflowNodeProcessResult process(WorkflowNodeDefinition node, String input, Long tenantId);
}
