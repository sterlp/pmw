package org.sterl.pmw.model;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class WorkflowFactory<T extends WorkflowContext> extends AbstractWorkflowFactory<WorkflowFactory<T>, T> {

    private final Workflow<T> workflow;
    
    public WorkflowFactory(String name, Supplier<T> newContextCreator) {
        this.workflow = new Workflow<>(name, newContextCreator);
    }
    
    public WorkflowFactory<T> next(Consumer<T> fn) {
        return step(new SequentialStep<>(defaultStepName(), fn));
    }
    
    
    public IfFactory<T> choose(Function<T, String> chooseFn) {
        return new IfFactory<>(this, chooseFn);
    }
    
    public Workflow<T> build() {
        workflow.setWorkflowSteps(this.workflowSteps.values());
        return workflow;
    }

    public WorkflowFactory<T> next(String name, Consumer<T> fn) {
        return step(new SequentialStep<>(name, fn));
    }

}
