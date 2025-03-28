package org.sterl.pmw.model;

import java.io.Serializable;
import java.util.Objects;

import lombok.Getter;

@Getter
public class SequentialStep<T extends Serializable> extends AbstractStep<T> {
    private final WorkflowFunction<T> fn;

    SequentialStep(String name, WorkflowFunction<T> fn) {
        this(name, null, fn);
    }

    SequentialStep(String name, String connectorLabel, WorkflowFunction<T> fn) {
        super(name, connectorLabel);
        Objects.requireNonNull(fn, "WorkflowFunction cannot be null.");
        this.fn = fn;
    }

    @Override
    public void apply(WorkflowContext<T> context) {
        Objects.requireNonNull(context, "WorkflowContext cannot be null.");
        fn.accept(context);
    }
}
