package org.sterl.pmw.model;

public interface Step<T extends AbstractWorkflowContext> {
    void apply(T c);
}
