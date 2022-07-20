package org.sterl.pmw.model;

public interface NamedStep<T extends AbstractWorkflowContext> extends Step<T> {
    String getName();
    void apply(T c);
}
