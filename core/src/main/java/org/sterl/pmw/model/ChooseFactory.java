package org.sterl.pmw.model;

import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;

/**
 * In contrast to an if the choose allows more then two selections.
 */
@RequiredArgsConstructor
public class ChooseFactory<StateType extends WorkflowState> extends AbstractWorkflowFactory<WorkflowFactory<StateType>, StateType> {
    private final WorkflowFactory<StateType> workflowFactory;
    private final WorkflowChooseFunction<StateType> chooseFn;
    private String name;

    public ChooseFactory<StateType> name(String name) {
        this.name = name;
        return this;
    }
    public ChooseFactory<StateType> ifSelected(String stepName, WorkflowFunction<StateType> fn) {
        addStep(new SequentialStep<>(stepName, fn));
        return this;
    }
    public ChooseFactory<StateType> ifSelected(String stepName, String connectorLabel, WorkflowFunction<StateType> fn) {
        addStep(new SequentialStep<>(stepName, connectorLabel, fn));
        return this;
    }
    public ChooseFactory<StateType> ifSelected(String stepName, Consumer<StateType> fn) {
        addStep(new SequentialStep<>(stepName, WorkflowFunction.of(fn)));
        return this;
    }
    public ChooseFactory<StateType> ifSelected(String stepName, String connectorLabel, Consumer<StateType> fn) {
        addStep(new SequentialStep<>(stepName, connectorLabel, WorkflowFunction.of(fn)));
        return this;
    }
    public WorkflowFactory<StateType> build() {
        if (name == null) name = workflowFactory.defaultStepName();
        workflowFactory.addStep(new ChooseStep<>(name, chooseFn, workflowSteps));
        return workflowFactory;
    }
}
