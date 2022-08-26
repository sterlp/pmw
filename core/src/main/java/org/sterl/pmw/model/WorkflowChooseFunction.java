package org.sterl.pmw.model;

import java.util.function.Consumer;

@FunctionalInterface
public interface WorkflowChooseFunction<StateType extends WorkflowState> {
    String apply(StateType state, WorkflowContext context);
}
