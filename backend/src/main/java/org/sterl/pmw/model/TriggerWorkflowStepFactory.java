package org.sterl.pmw.model;

import java.io.Serializable;
import java.time.Duration;
import java.util.function.Function;

public class TriggerWorkflowStepFactory<
    C extends StepHolder<C, T>, 
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

    @Override
    public WorkflowStep<T> buildStep() {
        if (description == null) {
            description = "Run **" + subWorkflow.getName();
            description += "** after " + this.delay;
        }
        return new TriggerWorkflowStep<>(id,
                description,
                connectorLabel,
                subWorkflow,
                fn,
                delay);
    }
}
