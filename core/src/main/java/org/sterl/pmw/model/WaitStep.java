package org.sterl.pmw.model;

import java.io.Serializable;
import java.time.Duration;
import java.util.function.Function;

import org.sterl.pmw.WorkflowService;

public class WaitStep<T extends Serializable> extends AbstractStep<T, T> {

    private final Function<T, Duration> fn;

    WaitStep(String name, Function<T, Duration> fn) {
        super(name, null);
        this.fn = fn;
    }

    @Override
    public T apply(T state, WorkflowContext context, WorkflowService<?> workflowService) {
        context.delayNextStepBy(fn.apply(state));
        return state;
    }
}
