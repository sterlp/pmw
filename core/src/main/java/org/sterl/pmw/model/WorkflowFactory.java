package org.sterl.pmw.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkflowFactory<T extends AbstractWorkflowContext> extends AbstractWorkflowFactory<WorkflowFactory<T>, T> {

    private final Workflow<T> workflow;
    
    public WorkflowFactory<T> newWorkflow(String name, Supplier<T> newContextCreator) {
        return new WorkflowFactory<T>(new Workflow<>(name, newContextCreator));
    }
    
    public WorkflowFactory<T> next(Consumer<T> fn) {
        return step(new SequentialStep<>(defaultStepName(), fn));
    }
    
    
    public IfFactory<T> choose(Function<T, String> chooseFn) {
        return new IfFactory<>(this, chooseFn);
    }

}
