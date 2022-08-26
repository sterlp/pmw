package org.sterl.pmw.model;

public interface WorkflowStep<StateType extends WorkflowState> {
    String getName();
    void apply(StateType state, WorkflowContext context);
    int getMaxRetryCount();
}
