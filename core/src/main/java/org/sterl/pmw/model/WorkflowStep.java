package org.sterl.pmw.model;

public interface WorkflowStep<T extends AbstractWorkflowContext> {
    void apply(T c);
}
