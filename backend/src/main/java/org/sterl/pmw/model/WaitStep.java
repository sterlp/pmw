package org.sterl.pmw.model;

import java.io.Serializable;
import java.time.Duration;
import java.util.function.Function;

import lombok.Getter;

public class WaitStep<T extends Serializable> extends AbstractStep<T> {

    private final Function<T, Duration> fn;
    @Getter
    private final boolean suspendNext;

    WaitStep(String id, String description, Function<T, Duration> fn, boolean suspendNext) {
        super(id, description, null, false);
        this.fn = fn;
        this.suspendNext = suspendNext;
    }

    @Override
    public void apply(WorkflowContext<T> context) {
        context.delayNextStepBy(fn.apply(context.data()));
        context.setSuspendNext(suspendNext);
    }
}
