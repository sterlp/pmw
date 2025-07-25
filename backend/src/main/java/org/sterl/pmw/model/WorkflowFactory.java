package org.sterl.pmw.model;

import java.io.Serializable;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import org.sterl.spring.persistent_tasks.api.RetryStrategy;

/**
 * @param <T> the workflow step type
 */
public class WorkflowFactory<T extends Serializable> implements StepHolder<T> {
    
    private final AtomicInteger stepIds = new AtomicInteger(0);
    private final StepContainer<T> steps = new StepContainer<>();

    private final Workflow<T> workflow;
    @SuppressWarnings("unused")
    private final Supplier<T> contextBuilder;
    private RetryStrategy retryStrategy = RetryStrategy.THREE_RETRIES;
    
    WorkflowFactory(Supplier<T> contextBuilder, Workflow<T> workflow) {
        this.workflow = workflow;
        this.contextBuilder = contextBuilder;
    }

    public SequentialStepFactory<WorkflowFactory<T>, T> next() {
        return new SequentialStepFactory<>(this);
    }
    public SequentialStepFactory<WorkflowFactory<T>, T> next(String id) {
        return new SequentialStepFactory<>(this).id(id);
    }
    public WorkflowFactory<T> next(WorkflowFunction<T> fn) {
        return next(nextStepId(), fn);
    }
    public WorkflowFactory<T> next(String name, WorkflowFunction<T> fn) {
        return next(new SequentialStep<T>(name, fn));
    }

    public <TS extends Serializable> WorkflowFactory<T> trigger(
            Workflow<TS> toTrigger, Function<T, TS> fn) {
        return next(new TriggerWorkflowStep<>(nextStepId(), "Start " + toTrigger.getName(), null, toTrigger, fn, Duration.ZERO));
    }
    
    public <TS extends Serializable> TriggerWorkflowStepFactory<WorkflowFactory<T>, T, TS> trigger(
            Workflow<TS> toTrigger) {
        var result = new TriggerWorkflowStepFactory<>(this, toTrigger);
        result.description("Start " + toTrigger.getName());
        return result;
    }
    
    public WorkflowFactory<T> sleep(Function<T, Duration> fn) {
        return next(new WaitStep<>(nextStepId(), "Wait using function", fn, false));
    }
    public WorkflowFactory<T> sleep(String id, Function<T, Duration> fn) {
        return next(new WaitStep<>(id, null, fn, false));
    }
    public WorkflowFactory<T> sleep(String id, String description, Function<T, Duration> fn) {
        return next(new WaitStep<>(id, description, fn, false));
    }
    public WorkflowFactory<T> sleep(String id, Duration duration) {
        return next(new WaitStep<>(id, "Wait for " + duration, (s) -> duration, false));
    }
    public WorkflowFactory<T> sleep(Duration duration) {
        return next(new WaitStep<>(nextStepId(), "Wait for " + duration, (s) -> duration, false));
    }
    public WorkflowFactory<T> stepRetryStrategy(RetryStrategy retryStrategy) {
        this.retryStrategy = retryStrategy;
        return this;
    }
    public Workflow<T> build() {
        workflow.setRetryStrategy(this.retryStrategy);
        workflow.setWorkflowSteps(this.steps.getSteps().values());
        return workflow;
    }
    /**
     * Allows to select multiple different named steps by returning the name of the step to execute.
     */
    public ChooseFactory<WorkflowFactory<T>, T> choose() {
        return new ChooseFactory<>(this);
    }
    /**
     * Allows to select multiple different named steps by returning the name of the step to execute.
     */
    public ChooseFactory<WorkflowFactory<T>, T> choose(WorkflowChooseFunction<T> chooseFn) {
        return new ChooseFactory<>(this).chooseFn(chooseFn);
    }
    /**
     * Allows to select multiple different named steps by returning the name of the step to execute.
     */
    public ChooseFactory<WorkflowFactory<T>, T> choose(String id, WorkflowChooseFunction<T> chooseFn) {
        return new ChooseFactory<>(this).chooseFn(chooseFn).id(id);
    }

    public String nextStepId() {
        return stepIds.addAndGet(10) + "";
    }

    public WorkflowFactory<T> next(WorkflowStep<T> s) {
        this.steps.next(s);
        return this;
    }
    public WorkflowFactory<T> useId(String id) {
        this.steps.useId(id);
        return this;
    }

    @Override
    public Map<String, WorkflowStep<T>> steps() {
        return this.steps.getSteps();
    }

    /**
     * This step will wait for a signal/resume using the {@link WorkflowContext#nextTaskId()}
     * and allows to update the workflow state for the next step.
     */
    public WorkflowFactory<T> await(Duration timeout) {
        return await(nextStepId(), timeout);
    }
    /**
     * This step will wait for a signal/resume using the {@link WorkflowContext#nextTaskId()}
     * and allows to update the workflow state for the next step.
     */
    public WorkflowFactory<T> await(String id, Duration timeout) {
        return next(new WaitStep<>(id, 
                "Suspend at most " + timeout, 
                s -> timeout, true));
    }
}
