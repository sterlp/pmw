package org.sterl.pmw.model;

public interface WorkflowStep<StateType extends WorkflowState> {
    /**
     * Name of the step itself, should be unique.
     */
    String getName();
    /**
     * Optional label for the connector leading to this step, just for readability
     */
    String getConnectorLabel();
    void apply(StateType state, WorkflowContext context);
    int getMaxRetryCount();
}
