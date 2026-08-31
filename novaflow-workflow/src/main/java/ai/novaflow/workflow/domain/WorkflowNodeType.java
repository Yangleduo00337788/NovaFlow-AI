package ai.novaflow.workflow.domain;

public final class WorkflowNodeType {

    public static final String START = "start";
    public static final String LLM = "llm";
    public static final String KNOWLEDGE = "knowledge";
    public static final String TOOL = "tool";
    public static final String CONDITION = "condition";
    public static final String AGENT = "agent";
    public static final String END = "end";

    private WorkflowNodeType() {
    }
}
