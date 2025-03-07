package org.sterl.pmw.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;

/**
 * In contrast to an if the choose allows more then two selections.
 */
@RequiredArgsConstructor
public class ChooseFactory<T extends Serializable, R extends Serializable, C extends Serializable> 
    extends AbstractWorkflowFactory<ChooseFactory> {

    private final WorkflowFactory workflowFactory;
    private final WorkflowChooseFunction<T> chooseFn;
    private String name;

    public ChooseFactory<T, R, C> name(String name) {
        this.name = name;
        return this;
    }
    public ChooseFactory<T, T, C> ifSelected(String stepName, Consumer<T> fn) {
        return addStep(new SequentialStep<T, T>(stepName, (s, c) -> { fn.accept(s); return s; }));
    }
    public ChooseFactory<T, R, C> ifSelected(String stepName, WorkflowFunction<T, R> fn) {
        return addStep(new SequentialStep<>(stepName, fn));
    }
    public ChooseFactory<T, R, C> ifSelected(String stepName, String connectorLabel, WorkflowFunction<T, R> fn) {
        return addStep(new SequentialStep<>(stepName, connectorLabel, fn));
    }
    public ChooseFactory<T, T, C> ifSelected(String stepName, String connectorLabel, Consumer<T> fn) {
        return addStep(new SequentialStep<T, T>(stepName, connectorLabel, (s, c) -> { fn.accept(s); return s; }));
    }
    public WorkflowFactory<T, C> build() {
        if (name == null) name = workflowFactory.defaultStepName();
        var steps = new LinkedHashMap<String, WorkflowStep<T, R>>();
        
        for (Entry<String, WorkflowStep<?, ?>> e : workflowSteps.entrySet()) {
            steps.put(e.getKey(), (WorkflowStep)e.getValue());
        }

        workflowFactory.addStep(new ChooseStep<T, R>(name, chooseFn, steps));
        return workflowFactory;
    }
}
