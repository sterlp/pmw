package org.sterl.pmw.model;

public interface NamedStep<T extends AbstractWorkflowContext> extends WorkflowStep<T> {
    String getName();
    void apply(T c);
}
