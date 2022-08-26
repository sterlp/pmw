package org.sterl.pmw.model;

import java.util.Objects;

import lombok.Getter;

@Getter
public class SequentialStep<StateType extends WorkflowState> extends AbstractStep<StateType> {
    private final WorkflowFunction<StateType> fn;

    SequentialStep(String name, WorkflowFunction<StateType> fn) {
        super(name);
        Objects.requireNonNull(fn, "WorkflowFunction cannot be null.");
        this.fn = fn;
    }

    @Override
    public void apply(StateType state, WorkflowContext context) {
        Objects.requireNonNull(state, "State cannot be null.");
        Objects.requireNonNull(context, "WorkflowContext cannot be null.");
        fn.accept(state, context);
    }
}
