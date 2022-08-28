package org.sterl.pmw.model;

@FunctionalInterface
public interface WorkflowChooseFunction<StateType extends WorkflowState> {
    String apply(StateType state, WorkflowContext context);
}
