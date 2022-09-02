package org.sterl.pmw.model;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class WorkflowFactory<StateType extends WorkflowState> extends AbstractWorkflowFactory<WorkflowFactory<StateType>, StateType> {

    private final Workflow<StateType> workflow;

    public WorkflowFactory(String name, Supplier<StateType> newContextCreator) {
        this.workflow = new Workflow<>(name, newContextCreator);
    }

    public WorkflowFactory<StateType> next(WorkflowFunction<StateType> fn) {
        return step(new SequentialStep<>(defaultStepName(), fn));
    }
    public WorkflowFactory<StateType> next(Consumer<StateType> fn) {
        return step(new SequentialStep<>(defaultStepName(), WorkflowFunction.of(fn)));
    }
    
    public WorkflowFactory<StateType> next(String name, WorkflowFunction<StateType> fn) {
        return step(new SequentialStep<>(name, fn));
    }
    public WorkflowFactory<StateType> next(String name, Consumer<StateType> fn) {
        return step(new SequentialStep<>(name, WorkflowFunction.of(fn)));
    }

    public IfFactory<StateType> choose(WorkflowChooseFunction<StateType> chooseFn) {
        return new IfFactory<>(this, chooseFn);
    }

    public IfFactory<StateType> choose(String name, WorkflowChooseFunction<StateType> chooseFn) {
        return new IfFactory<>(this, chooseFn).name(name);
    }

    public Workflow<StateType> build() {
        workflow.setWorkflowSteps(this.workflowSteps.values());
        return workflow;
    }
}
