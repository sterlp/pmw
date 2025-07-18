package org.sterl.pmw.model;

import java.io.Serializable;
import java.time.Duration;
import java.util.function.Function;

public class TriggerWorkflowStepFactory<
    C extends StepHolder<T>, 
    T extends Serializable, 
    R extends Serializable> extends AbstractStepFactory<TriggerWorkflowStepFactory<C, T, R>, C, T> {

    private final Workflow<R> subWorkflow;
    private Function<T, R> fn;
    private Duration delay = Duration.ZERO;
    
    public TriggerWorkflowStepFactory(C context, Workflow<R> subWorkflow) {
        super(context);
        this.subWorkflow = subWorkflow;
    }
    
    public TriggerWorkflowStepFactory<C, T, R> function(Function<T, R> value) {
        fn = value;
        return this;
    }
    
    public TriggerWorkflowStepFactory<C, T, R> delay(Duration value) {
        delay = value;
        return this;
    }
    
    public C build() {
        if (id == null) id = context.nextStepId();
        context.next(new TriggerWorkflowStep<>(id,
                                                 description,
                                                 connectorLabel,
                                                 subWorkflow,
                                                 fn,
                                                 delay));
        return context;
    }
}
