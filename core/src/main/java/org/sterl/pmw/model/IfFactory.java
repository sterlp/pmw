package org.sterl.pmw.model;

import java.util.function.Consumer;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IfFactory<T extends AbstractWorkflowContext> extends AbstractWorkflowFactory<WorkflowFactory<T>, T> {
    private final WorkflowFactory<T> workflowFactory;
    private final Function<T, String> chooseFn;
    private String name;
    
    public IfFactory<T> name(String name) {
        this.name = name;
        return this;
    }
    public IfFactory<T> ifSelected(String value, Consumer<T> fn) {
        step(new SequentialStep<>(value, fn));
        return this;
    }
    
    public WorkflowFactory<T> endChoose() {
        if (name == null) name = defaultStepName();
        workflowFactory.step(new IfStep<T>(name, chooseFn, workflowSteps));
        return workflowFactory;
    }
}
