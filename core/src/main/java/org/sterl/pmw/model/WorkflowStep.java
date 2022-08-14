package org.sterl.pmw.model;

public interface WorkflowStep<T extends WorkflowContext> {
    String getName();
    void apply(T c);
    int getMaxRetryCount();
}
