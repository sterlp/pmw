package org.sterl.pmw.model;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class WorkflowFactory<StateType extends WorkflowState> extends AbstractWorkflowFactory<WorkflowFactory<StateType>, StateType> {

    private final Workflow<StateType> workflow;

    public WorkflowFactory(String name, Supplier<StateType> newContextCreator) {
        this.workflow = new Workflow<>(name, newContextCreator);
    }

    public WorkflowFactory<StateType> next(WorkflowFunction<StateType> fn) {
        return addStep(new SequentialStep<>(defaultStepName(), fn));
    }
    public WorkflowFactory<StateType> next(Consumer<StateType> fn) {
        return addStep(new SequentialStep<>(defaultStepName(), WorkflowFunction.of(fn)));
    }
    public WorkflowFactory<StateType> next(String name, WorkflowFunction<StateType> fn) {
        return addStep(new SequentialStep<>(name, fn));
    }
    public WorkflowFactory<StateType> next(String name, Consumer<StateType> fn) {
        return addStep(new SequentialStep<>(name, WorkflowFunction.of(fn)));
    }

    public <TriggeredWorkflowStateType extends WorkflowState> WorkflowFactory<StateType> trigger(
            Workflow<TriggeredWorkflowStateType> toTriger, Function<StateType, TriggeredWorkflowStateType> fn) {
        addStep(new TriggerWorkflowStep<>(this.defaultStepName("Trigger " + toTriger.getName()), null, toTriger, fn, Duration.ZERO));
        return this;
    }

    public WorkflowFactory<StateType> sleep(Function<StateType, Duration> fn) {
        return addStep(new WaitStep<>("Sleep", fn));
    }
    public WorkflowFactory<StateType> sleep(String name, Function<StateType, Duration> fn) {
        return addStep(new WaitStep<>(name, fn));
    }
    public WorkflowFactory<StateType> sleep(Duration duration) {
        return addStep(new WaitStep<>("Sleep for " + duration, (s) -> duration));
    }

    /**
     * Allows to select multiple different named steps by returning the name of the step to execute.
     */
    public ChooseFactory<StateType> choose(WorkflowChooseFunction<StateType> chooseFn) {
        return new ChooseFactory<>(this, chooseFn);
    }
    /**
     * Allows to select multiple different named steps by returning the name of the step to execute.
     * @param name a optional name for better readability and export to UML
     */
    public ChooseFactory<StateType> choose(String name, WorkflowChooseFunction<StateType> chooseFn) {
        return new ChooseFactory<>(this, chooseFn).name(name);
    }

    public Workflow<StateType> build() {
        workflow.setWorkflowSteps(this.workflowSteps.values());
        return workflow;
    }
}
