package org.sterl.pmw.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import lombok.RequiredArgsConstructor;

/**
 * In contrast to an if the choose allows more then two selections.
 */
@RequiredArgsConstructor
public class ChooseFactory<T extends Serializable> 
    extends AbstractWorkflowFactory<ChooseFactory<T>, T> {

    private final WorkflowFactory<T> workflowFactory;
    private final WorkflowChooseFunction<T> chooseFn;
    private String name;

    public ChooseFactory<T> name(String name) {
        this.name = name;
        return this;
    }
    public ChooseFactory<T> ifSelected(String stepName, WorkflowFunction<T> fn) {
        return addStep(new SequentialStep<>(stepName, fn));
    }
    public ChooseFactory<T> ifSelected(String stepName, String connectorLabel, WorkflowFunction<T> fn) {
        return addStep(new SequentialStep<>(stepName, connectorLabel, fn));
    }
    public WorkflowFactory<T> build() {
        if (name == null) name = workflowFactory.defaultStepName();
        var steps = new LinkedHashMap<String, WorkflowStep<T>>();
        
        for (Entry<String, WorkflowStep<T>> e : workflowSteps.entrySet()) {
            steps.put(e.getKey(), e.getValue());
        }

        workflowFactory.addStep(new ChooseStep<T>(name, chooseFn, steps));
        return workflowFactory;
    }
}
