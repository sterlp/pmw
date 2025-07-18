package org.sterl.pmw.model;

import java.io.Serializable;
import java.time.Duration;
import java.util.function.Function;

public class WaitStep<T extends Serializable> extends AbstractStep<T> {

    private final Function<T, Duration> fn;

    WaitStep(String id, String description, Function<T, Duration> fn) {
        super(id, description, null);
        this.fn = fn;
    }

    @Override
    public void apply(WorkflowContext<T> context) {
        context.delayNextStepBy(fn.apply(context.data()));
    }
}
