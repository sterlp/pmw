package org.sterl.pmw.model;

import java.util.LinkedHashMap;

public abstract class AbstractWorkflowFactory<FactoryType
    extends AbstractWorkflowFactory<FactoryType, StateType>, StateType extends WorkflowState> {

    protected final LinkedHashMap<String, WorkflowStep<StateType>> workflowSteps = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    public FactoryType step(WorkflowStep<StateType> s) {
        var old = workflowSteps.put(s.getName(), s);
        if (old != null) throw new IllegalArgumentException("WorkflowStep with name "
                + s.getName() + " already exists.");
        return (FactoryType)this;
    }

    protected String defaultStepName() {
        return defaultStepName("Step ");
    }
    
    protected String defaultStepName(String prefix) {
        return prefix + workflowSteps.size();
    }
}
