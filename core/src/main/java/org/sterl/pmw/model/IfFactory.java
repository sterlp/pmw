package org.sterl.pmw.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IfFactory<StateType extends WorkflowState> extends AbstractWorkflowFactory<WorkflowFactory<StateType>, StateType> {
    private final WorkflowFactory<StateType> workflowFactory;
    private final WorkflowChooseFunction<StateType> chooseFn;
    private String name;
    
    public IfFactory<StateType> name(String name) {
        this.name = name;
        return this;
    }
    public IfFactory<StateType> ifSelected(String value, WorkflowFunction<StateType> fn) {
        step(new SequentialStep<>(value, fn));
        return this;
    }
    public WorkflowFactory<StateType> build() {
        if (name == null) name = workflowFactory.defaultStepName();
        workflowFactory.step(new IfStep<StateType>(name, chooseFn, workflowSteps));
        return workflowFactory;
    }
}
