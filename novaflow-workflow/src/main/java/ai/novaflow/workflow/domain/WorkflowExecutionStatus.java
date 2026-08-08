package ai.novaflow.workflow.domain;

public final class WorkflowExecutionStatus {

    public static final int RUNNING = 0;
    public static final int SUCCESS = 1;
    public static final int FAILED = 2;
    public static final int TIMEOUT = 3;

    private WorkflowExecutionStatus() {
    }
}
