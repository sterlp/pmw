package org.sterl.pmw.model;

import java.time.Duration;
import java.util.function.Function;

import org.sterl.pmw.boundary.WorkflowService;

public class WaitStep<StateType extends WorkflowState> extends AbstractStep<StateType> {

    private final Function<StateType, Duration> fn;

    WaitStep(String name, Function<StateType, Duration> fn) {
        super(name, null);
        this.fn = fn;
        this.maxRetryCount = 0;
    }

    @Override
    public void apply(StateType state, WorkflowContext context, WorkflowService<?> workflowService) {
        context.delayNextStepBy(fn.apply(state));
    }
}
