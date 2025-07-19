package org.sterl.pmw.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;

@Getter
public class ChooseStep<T extends Serializable> extends AbstractStep<T> {

    private final WorkflowChooseFunction<T> chooseFn;
    private final Map<String, WorkflowStep<T>> subSteps;

    ChooseStep(String id, String description, String connectorLabel, boolean transactional, 
            WorkflowChooseFunction<T> chooseFn, Map<String, WorkflowStep<T>> subSteps) {
        super(id, description, connectorLabel, transactional);
        this.chooseFn = chooseFn;
        this.subSteps = subSteps;
    }

    @Override
    public void apply(WorkflowContext<T> context) {
        final String stepId = chooseFn.apply(context.data());
        WorkflowStep<T> selectedStep = subSteps.get(stepId);

        if (selectedStep == null) {
            throw new IllegalStateException("No step with ID " + stepId + " exists. Select one of " + subSteps.keySet());
        }

        selectedStep.apply(context);
    }

    public Map<String, WorkflowStep<T>> getSubSteps() {
        return new LinkedHashMap<>(this.subSteps);
    }
}
