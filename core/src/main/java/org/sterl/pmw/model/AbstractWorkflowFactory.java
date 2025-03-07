package org.sterl.pmw.model;

import java.io.Serializable;
import java.util.LinkedHashMap;

public abstract class AbstractWorkflowFactory<FactoryType> {

    protected final LinkedHashMap<String, WorkflowStep<?, ?>> workflowSteps = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    public <T extends Serializable, R extends Serializable> FactoryType addStep(WorkflowStep<T, R> s) {
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
