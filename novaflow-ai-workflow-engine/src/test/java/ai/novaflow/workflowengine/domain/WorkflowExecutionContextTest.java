package ai.novaflow.workflowengine.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowExecutionContextTest {

    @Test
    void recordsSuccessfulStepAndUpdatesPayload() {
        WorkflowExecutionContext context = new WorkflowExecutionContext();
        context.setPayload("input");
        WorkflowNodeDefinition node = WorkflowNodeDefinition.builder()
                .nodeId("n1")
                .nodeType("llm")
                .nodeName("LLM")
                .build();
        context.getNodeMap().put("n1", node);

        context.recordStep(
                node,
                "input",
                WorkflowNodeProcessResult.builder().success(true).output("output").tokensUsed(10).build(),
                5
        );

        assertEquals("output", context.getPayload());
        assertEquals(10, context.getTotalTokens());
        assertEquals(1, context.getSteps().size());
        assertEquals(1, context.getSteps().getFirst().getStatus());
    }

    @Test
    void marksFailedWhenStepFails() {
        WorkflowExecutionContext context = new WorkflowExecutionContext();
        WorkflowNodeDefinition node = WorkflowNodeDefinition.builder()
                .nodeId("n1")
                .nodeType("tool")
                .nodeName("Tool")
                .build();

        context.recordStep(
                node,
                "input",
                WorkflowNodeProcessResult.builder().success(false).errorMessage("boom").build(),
                3
        );

        assertTrue(context.isFailed());
        assertEquals("boom", context.getErrorMessage());
    }
}
