package org.sterl.pmw.model;

import java.io.Serializable;
import java.util.Objects;

import org.sterl.pmw.WorkflowService;

import lombok.Getter;

@Getter
public class SequentialStep<T extends Serializable, R extends Serializable> extends AbstractStep<T, R> {
    private final WorkflowFunction<T, R> fn;

    SequentialStep(String name, WorkflowFunction<T, R> fn) {
        this(name, null, fn);
    }

    SequentialStep(String name, String connectorLabel, WorkflowFunction<T, R> fn) {
        super(name, connectorLabel);
        Objects.requireNonNull(fn, "WorkflowFunction cannot be null.");
        this.fn = fn;
    }

    @Override
    public R apply(T state, WorkflowContext context, WorkflowService<?> workflowService) {
        Objects.requireNonNull(state, "WorkflowState cannot be null.");
        Objects.requireNonNull(context, "WorkflowContext cannot be null.");
        return fn.accept(state, context);
    }
}
