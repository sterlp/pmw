package org.sterl.pmw.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class IfStep<T extends AbstractWorkflowContext> implements WorkflowStep<T> {

    private final Function<T, String> decideFunction;
    private final Workflow<T> parent;
    private final Map<String, WorkflowStep<T>> subSteps = new LinkedHashMap<>();
    
    public IfStep<T> ifSelected(String value, WorkflowStep<T> step) {
        WorkflowStep<T> oldStep = subSteps.put(value, step);
        if (oldStep != null) throw new IllegalArgumentException("WorkflowStep with name " 
                + value + " already exists.");
        return this;
    }

    public Workflow<T> end() {
        return this.parent;
    }

    @Override
    public void apply(T c) {
        String stepName = decideFunction.apply(c);
        WorkflowStep<T> selectedStep = subSteps.get(stepName);

        if (selectedStep == null) throw new IllegalStateException("No step with name " 
                    + stepName + " exists anymore. Select one of " + subSteps.keySet());

        selectedStep.apply(c);
    }
}
