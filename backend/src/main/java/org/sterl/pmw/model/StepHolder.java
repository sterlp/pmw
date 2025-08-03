package org.sterl.pmw.model;

import java.io.Serializable;
import java.util.Map;

/**
 * @param <F> the factory type
 * @param <T> the function argument type
 */
public interface StepHolder<F extends StepHolder<F, T>, T extends Serializable> {

    default SequentialStepFactory<F, T> next() {
        return new SequentialStepFactory<F, T>((F) this);
    }
    
    default <R extends Serializable> TriggerWorkflowStepFactory<F, T, R> forkWorkflow(Workflow<R> workflow) {
        return new TriggerWorkflowStepFactory<>((F) this, workflow);
    }
    
    default SequentialStepFactory<F, T> next(String id) {
        return new SequentialStepFactory<>((F) this).id(id);
    }
    default F next(WorkflowFunction<T> fn) {
        return next(nextStepId(), fn);
    }
    
    default F next(String name, WorkflowFunction<T> fn) {
        return next(new SequentialStep<T>(name, fn));
    }

    /**
     * @param s The step to add
     * @return the holder for chaining
     */
    F next(WorkflowStep<T> s);

    Map<String, WorkflowStep<T>> steps();

    String nextStepId();
}
