package org.sterl.pmw.model;

import java.util.Map;
import java.util.function.Function;

import lombok.Getter;

@Getter
public class IfStep<T extends AbstractWorkflowContext> extends AbstractStep<T> {

    private final Function<T, String> chooseFn;
    private final Map<String, WorkflowStep<T>> subSteps;
    
    IfStep(String name, Function<T, String> chooseFn, Map<String, WorkflowStep<T>> subSteps) {
        super(name);
        this.chooseFn = chooseFn;
        this.subSteps = subSteps;
    }
    

    @Override
    public void apply(T c) {
        String stepName = chooseFn.apply(c);
        WorkflowStep<T> selectedStep = subSteps.get(stepName);

        if (selectedStep == null) throw new IllegalStateException("No step with name " 
                    + stepName + " exists anymore. Select one of " + subSteps.keySet());

        selectedStep.apply(c);
    }
}
