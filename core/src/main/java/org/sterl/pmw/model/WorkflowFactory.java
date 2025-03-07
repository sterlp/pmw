package org.sterl.pmw.model;

import java.io.Serializable;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.sterl.spring.persistent_tasks.api.RetryStrategy;

/**
 * @param <T> the initial step type
 */
@SuppressWarnings("rawtypes")
public class WorkflowFactory<T extends Serializable, C extends Serializable> 
    extends AbstractWorkflowFactory<WorkflowFactory> {
    
    private final Workflow<C> workflow;
    @SuppressWarnings("unused")
    private final Supplier<T> contextBuilder;
    private RetryStrategy retryStrategy = RetryStrategy.THREE_RETRIES;
    
    WorkflowFactory(Supplier<T> contextBuilder, Workflow<C> workflow) {
        this.workflow = workflow;
        this.contextBuilder = contextBuilder;
    }

    public <R extends Serializable> WorkflowFactory<R, C> next(WorkflowFunction<T, R> fn) {
        return next(defaultStepName(), fn);
    }
    public <R extends Serializable> WorkflowFactory<R, C> next(String name, WorkflowFunction<T, R> fn) {
        return addStep(new SequentialStep<T, R>(name, fn));
    }
    public WorkflowFactory<T, C> next(Consumer<T> fn) {
        return next(defaultStepName(), fn);
    }
    public WorkflowFactory<T, C> next(String name, Consumer<T> fn) {
        var step = new SequentialStep<T, T>(name, (s, c) -> { fn.accept(s); return s; });
        return addStep(step);
    }

    public <TriggeredWorkflowStateType extends Serializable> WorkflowFactory<T, C> trigger(
            Workflow<TriggeredWorkflowStateType> toTrigger, Function<T, TriggeredWorkflowStateType> fn) {
        addStep(new TriggerWorkflowStep<>(this.defaultStepName("Trigger " + toTrigger.getName()), null, toTrigger, fn, Duration.ZERO));
        return this;
    }

    public WorkflowFactory<T, C> sleep(Function<T, Duration> fn) {
        return addStep(new WaitStep<>("Sleep", fn));
    }
    public WorkflowFactory<T, C> sleep(String name, Function<T, Duration> fn) {
        return addStep(new WaitStep<>(name, fn));
    }
    public WorkflowFactory<T, C> sleep(Duration duration) {
        return addStep(new WaitStep<>("Sleep for " + duration, (s) -> duration));
    }
    public WorkflowFactory<T, C> stepRetryStrategy(RetryStrategy retryStrategy) {
        this.retryStrategy = retryStrategy;
        return this;
    }
    public Workflow<C> build() {
        workflow.setRetryStrategy(this.retryStrategy);
        workflow.setWorkflowSteps(this.workflowSteps.values());
        return workflow;
    }
    /**
     * Allows to select multiple different named steps by returning the name of the step to execute.
     */
    public <R extends Serializable> ChooseFactory<T, C, T> choose(WorkflowChooseFunction<T> chooseFn) {
        return new ChooseFactory<T, C, T>(this, chooseFn);
    }
    /**
     * Allows to select multiple different named steps by returning the name of the step to execute.
     */
    public <R extends Serializable> ChooseFactory<T, C, T> choose(String name, WorkflowChooseFunction<T> chooseFn) {
        return new ChooseFactory<T, C, T>(this, chooseFn).name(name);
    }
    /**
     * Allows to select multiple different named steps by returning the name of the step to execute.
     */
    public <R extends Serializable> ChooseFactory<T, C, R> choose(Class<R> clazz, WorkflowChooseFunction<T> chooseFn) {
        return new ChooseFactory<T, C, R>(this, chooseFn);
    }
    /**
     * Allows to select multiple different named steps by returning the name of the step to execute.
     * @param name a optional name for better readability and export to UML
     */
    public <R extends Serializable> ChooseFactory<T, C, R> choose(String name, Class<R> clazz, WorkflowChooseFunction<T> chooseFn) {
        return new ChooseFactory<T, C, R>(this, chooseFn).name(name);
    }
}
