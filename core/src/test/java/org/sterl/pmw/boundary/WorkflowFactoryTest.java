package org.sterl.pmw.boundary;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sterl.pmw.model.IfStep;
import org.sterl.pmw.model.SimpleWorkflowState;
import org.sterl.pmw.model.Workflow;

public class WorkflowFactoryTest {

    @Test
    void testDefaultName() {
        // GIVEN
        Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow",
                () ->  new SimpleWorkflowState())
            .choose((s, c) -> "right")
                .ifSelected("left", (s, c) -> {})
                .ifSelected("right", (s, c) -> {})
                .build()
            .next((s, c) -> {})
            .next("foo", (s, c) -> {})
            .build();

        // THEN
        assertThat(w.getStepByPosition(0).getName()).isEqualTo("Step 0");
        assertThat(((IfStep<?>)w.getStepByPosition(0)).getSubSteps().get("left").getName()).isEqualTo("left");
        assertThat(((IfStep<?>)w.getStepByPosition(0)).getSubSteps().get("right").getName()).isEqualTo("right");
        assertThat(w.getStepByPosition(1).getName()).isEqualTo("Step 1");
        assertThat(w.getStepByPosition(2).getName()).isEqualTo("foo");

    }
}
