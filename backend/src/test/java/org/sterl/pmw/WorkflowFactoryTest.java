package org.sterl.pmw;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sterl.pmw.model.ChooseStep;
import org.sterl.pmw.model.Workflow;

public class WorkflowFactoryTest {

    @Test
    void testDefaultName() {
        // GIVEN
        Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow",
                SimpleWorkflowState::new)
            .choose(s -> "right")
                .ifSelected("left", s -> {})
                .ifSelected("right", s -> {})
                .build()
            .next(s -> {})
            .next("foo", s -> {})
            .build();

        // THEN
        assertThat(w.getStepByPosition(0).getId()).isEqualTo("10");
        assertThat(((ChooseStep<?>)w.getStepByPosition(0)).getSubSteps().get("left").getId()).isEqualTo("left");
        assertThat(((ChooseStep<?>)w.getStepByPosition(0)).getSubSteps().get("right").getId()).isEqualTo("right");
        assertThat(w.getStepByPosition(1).getId()).isEqualTo("20");
        assertThat(w.getStepByPosition(2).getId()).isEqualTo("foo");

    }
}
