package org.sterl.pmw.model;

import java.util.LinkedHashMap;

public abstract class AbstractWorkflowFactory<F extends AbstractWorkflowFactory<F, T>, T extends AbstractWorkflowContext> {

    protected final LinkedHashMap<String, WorkflowStep<T>> workflowSteps = new LinkedHashMap<>();
    
    public F step(WorkflowStep<T> s) {
        var old = workflowSteps.put(s.getName(), s);
        if (old != null) throw new IllegalArgumentException("WorkflowStep with name " 
                + s.getName() + " already exists.");
        return (F)this;
    }
    
    protected String defaultStepName() {
        return "Step " + workflowSteps.size() + 1;
    }
}
