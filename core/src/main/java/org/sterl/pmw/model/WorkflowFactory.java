package org.sterl.pmw.model;

import java.io.Serializable;
import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

import org.sterl.spring.persistent_tasks.api.RetryStrategy;

/**
 * @param <T> the workflow step type
 */
public class WorkflowFactory<T extends Serializable> 
    extends AbstractWorkflowFactory<WorkflowFactory<T>, T> {
    
    private final Workflow<T> workflow;
    @SuppressWarnings("unused")
    private final Supplier<T> contextBuilder;
    private RetryStrategy retryStrategy = RetryStrategy.THREE_RETRIES;
    
    WorkflowFactory(Supplier<T> contextBuilder, Workflow<T> workflow) {
        this.workflow = workflow;
        this.contextBuilder = contextBuilder;
    }

    public WorkflowFactory<T> next(WorkflowFunction<T> fn) {
        return next(defaultStepName(), fn);
    }
    public WorkflowFactory<T> next(String name, WorkflowFunction<T> fn) {
        return addStep(new SequentialStep<T>(name, fn));
    }

    public <TriggeredWorkflowStateType extends Serializable> WorkflowFactory<T> trigger(
            Workflow<TriggeredWorkflowStateType> toTrigger, Function<T, TriggeredWorkflowStateType> fn) {
        addStep(new TriggerWorkflowStep<>(this.defaultStepName("Trigger " + toTrigger.getName()), null, toTrigger, fn, Duration.ZERO));
        return this;
    }

    public WorkflowFactory<T> sleep(Function<T, Duration> fn) {
        return addStep(new WaitStep<>("Sleep", fn));
    }
    public WorkflowFactory<T> sleep(String name, Function<T, Duration> fn) {
        return addStep(new WaitStep<>(name, fn));
    }
    public WorkflowFactory<T> sleep(Duration duration) {
        return addStep(new WaitStep<>("Sleep for " + duration, (s) -> duration));
    }
    public WorkflowFactory<T> stepRetryStrategy(RetryStrategy retryStrategy) {
        this.retryStrategy = retryStrategy;
        return this;
    }
    public Workflow<T> build() {
        workflow.setRetryStrategy(this.retryStrategy);
        workflow.setWorkflowSteps(this.workflowSteps.values());
        return workflow;
    }
    /**
     * Allows to select multiple different named steps by returning the name of the step to execute.
     */
    public ChooseFactory<T> choose(WorkflowChooseFunction<T> chooseFn) {
        return choose(null, chooseFn);
    }
    /**
     * Allows to select multiple different named steps by returning the name of the step to execute.
     */
    public ChooseFactory<T> choose(String name, WorkflowChooseFunction<T> chooseFn) {
        return new ChooseFactory<T>(this, chooseFn).name(name);
    }
}
