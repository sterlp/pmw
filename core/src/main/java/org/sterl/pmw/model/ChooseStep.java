package org.sterl.pmw.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;

@Getter
public class ChooseStep<T extends Serializable> extends AbstractStep<T> {

    private final WorkflowChooseFunction<T> chooseFn;
    private final Map<String, WorkflowStep<T>> subSteps;

    ChooseStep(String name, WorkflowChooseFunction<T> chooseFn, Map<String, WorkflowStep<T>> subSteps) {
        super(name, null);
        this.chooseFn = chooseFn;
        this.subSteps = subSteps;
    }

    @Override
    public void apply(WorkflowContext<T> context) {
        final String stepName = chooseFn.apply(context.state());
        WorkflowStep<T> selectedStep = subSteps.get(stepName);

        if (selectedStep == null) {
            throw new IllegalStateException("No step with name " + stepName + " exists. Select one of " + subSteps.keySet());
        }

        selectedStep.apply(context);
    }

    public Map<String, WorkflowStep<T>> getSubSteps() {
        return new LinkedHashMap<>(this.subSteps);
    }
}
