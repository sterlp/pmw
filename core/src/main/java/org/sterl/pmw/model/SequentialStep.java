package org.sterl.pmw.model;

import java.util.Objects;

import lombok.Getter;

@Getter
public class SequentialStep<StateType extends WorkflowState> extends AbstractStep<StateType> {
    private final WorkflowFunction<StateType> fn;

    SequentialStep(String name, WorkflowFunction<StateType> fn) {
        this(name, null, fn);
    }
    
    SequentialStep(String name, String connectorLabel, WorkflowFunction<StateType> fn) {
        super(name, connectorLabel);
        Objects.requireNonNull(fn, "WorkflowFunction cannot be null.");
        this.fn = fn;
    }

    @Override
    public void apply(StateType state, WorkflowContext context) {
        Objects.requireNonNull(state, "WorkflowState cannot be null.");
        Objects.requireNonNull(context, "WorkflowContext cannot be null.");
        fn.accept(state, context);
    }
}
