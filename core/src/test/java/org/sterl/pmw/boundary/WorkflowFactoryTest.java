package org.sterl.pmw.boundary;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sterl.pmw.model.IfStep;
import org.sterl.pmw.model.SimpleWorkflowContext;
import org.sterl.pmw.model.Workflow;

public class WorkflowFactoryTest {

    @Test
    void testDefaultName() {
        // GIVEN
        Workflow<SimpleWorkflowContext> w = Workflow.builder("test-workflow", 
                () ->  new SimpleWorkflowContext())
            .choose(c -> "right")
            .ifSelected("left", c -> {})
            .ifSelected("right", c -> {})
            .build()
            .next(c -> {})
            .next("foo", c -> {})
            .build();
        
        // THEN
        assertThat(w.getStepByPosition(0).getName()).isEqualTo("Step 0");
        assertThat(((IfStep<?>)w.getStepByPosition(0)).getSubSteps().get("left").getName()).isEqualTo("left");
        assertThat(((IfStep<?>)w.getStepByPosition(0)).getSubSteps().get("right").getName()).isEqualTo("right");
        assertThat(w.getStepByPosition(1).getName()).isEqualTo("Step 1");
        assertThat(w.getStepByPosition(2).getName()).isEqualTo("foo");
        
    }
}
