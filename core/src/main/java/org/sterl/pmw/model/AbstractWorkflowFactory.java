package org.sterl.pmw.model;

import java.io.Serializable;
import java.util.LinkedHashMap;

public abstract class AbstractWorkflowFactory<FactoryType, T extends Serializable> {

    protected final LinkedHashMap<String, WorkflowStep<T>> workflowSteps = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    public FactoryType addStep(WorkflowStep<T> s) {
        var old = workflowSteps.put(s.getName(), s);
        if (old != null) throw new IllegalArgumentException("WorkflowStep with name "
                + s.getName() + " already exists.");
        return (FactoryType)this;
    }
    protected String defaultStepName() {
        return defaultStepName("Step");
    }
    protected String defaultStepName(String name) {
        return workflowSteps.size() + ". " + name;
    }
}
