package org.sterl.pmw.component;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.sterl.pmw.model.SimpleWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowState;

class SerializationUtilTest {
    
    private class ExtendetSimpleWorkflowState extends SimpleWorkflowState {}
    private class OtherSimpleWorkflowState implements WorkflowState {}

    @Test
    void testCorrectStateType() {
        Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow",
                () ->  new SimpleWorkflowState())
                .build();

        SerializationUtil.verifyStateType(w, null);
        SerializationUtil.verifyStateType(w, new SimpleWorkflowState());
        SerializationUtil.verifyStateType(w, new ExtendetSimpleWorkflowState());
    }
    
    @Test
    void testInCorrectStateType() {
        Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow",
                () ->  new SimpleWorkflowState())
                .build();

        assertThrows(IllegalArgumentException.class, () -> SerializationUtil.verifyStateType(w, new OtherSimpleWorkflowState()));
    }
}
