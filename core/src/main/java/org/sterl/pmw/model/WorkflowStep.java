package org.sterl.pmw.model;

public interface WorkflowStep<T extends AbstractWorkflowContext> {
    String getName();
    void apply(T c);
    int getMaxRetryCount();
}
