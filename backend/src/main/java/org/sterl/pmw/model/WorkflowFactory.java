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
    public WorkflowFactory<T> next(WorkflowFunction<T> fn) {
        return next(nextStepId(), fn);
    }
    public WorkflowFactory<T> next(String name, WorkflowFunction<T> fn) {
        addStep(new SequentialStep<T>(name, fn));
        return this;
    }

    public <TS extends Serializable> WorkflowFactory<T> trigger(
            Workflow<TS> toTrigger, Function<T, TS> fn) {
        addStep(new TriggerWorkflowStep<>(nextStepId(), "Start " + toTrigger.getName(), null, toTrigger, fn, Duration.ZERO));
        return this;
    }
    
    public <TS extends Serializable> TriggerWorkflowStepFactory<WorkflowFactory<T>, T, TS> trigger(
            Workflow<TS> toTrigger) {
        var result = new TriggerWorkflowStepFactory<>(this, toTrigger);
        result.description("Start " + toTrigger.getName());
        return result;
    }
    
    public WorkflowFactory<T> sleep(Function<T, Duration> fn) {
        addStep(new WaitStep<>(nextStepId(), "Sleep", fn));
        return this;
    }
    public WorkflowFactory<T> sleep(String id, Function<T, Duration> fn) {
        addStep(new WaitStep<>(id, "Sleep", fn));
        return this;
    }
    public WorkflowFactory<T> sleep(Duration duration) {
        addStep(new WaitStep<>(nextStepId(), "Sleep for " + duration, (s) -> duration));
        return this;
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

    public void addStep(WorkflowStep<T> s) {
        this.steps.addStep(s);
    }
    public WorkflowFactory<T> useId(String id) {
        this.steps.useId(id);
        return this;
    }

    @Override
    public Map<String, WorkflowStep<T>> steps() {
        return this.steps.getSteps();
    }
}
