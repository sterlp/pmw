package org.sterl.pmw.boundary;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sterl.pmw.model.ChooseStep;
import org.sterl.pmw.model.SimpleWorkflowState;
import org.sterl.pmw.model.Workflow;

public class WorkflowFactoryTest {

    @Test
    void testDefaultName() {
        // GIVEN
        Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow",
                () ->  new SimpleWorkflowState())
            .choose(s -> "right")
                .ifSelected("left", (s, c) -> {})
                .ifSelected("right", (s, c) -> {})
                .build()
            .next((s, c) -> {})
            .next("foo", (s, c) -> {})
            .build();

        // THEN
        assertThat(w.getStepByPosition(0).getName()).isEqualTo("0. Step");
        assertThat(((ChooseStep<?>)w.getStepByPosition(0)).getSubSteps().get("left").getName()).isEqualTo("left");
        assertThat(((ChooseStep<?>)w.getStepByPosition(0)).getSubSteps().get("right").getName()).isEqualTo("right");
        assertThat(w.getStepByPosition(1).getName()).isEqualTo("1. Step");
        assertThat(w.getStepByPosition(2).getName()).isEqualTo("foo");

    }
}
