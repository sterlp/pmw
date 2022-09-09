package org.sterl.pmw.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.sterl.pmw.boundary.WorkflowService;

import lombok.Getter;

@Getter
public class ChooseStep<StateType extends WorkflowState> extends AbstractStep<StateType> {

    private final WorkflowChooseFunction<StateType> chooseFn;
    private final Map<String, WorkflowStep<StateType>> subSteps;

    ChooseStep(String name, WorkflowChooseFunction<StateType> chooseFn, Map<String, WorkflowStep<StateType>> subSteps) {
        super(name, null);
        this.chooseFn = chooseFn;
        this.subSteps = subSteps;
    }

    @Override
    public void apply(StateType state, WorkflowContext context, WorkflowService<?> workflowService) {
        final String stepName = chooseFn.apply(state);
        WorkflowStep<StateType> selectedStep = subSteps.get(stepName);

        if (selectedStep == null) throw new IllegalStateException("No step with name "
                    + stepName + " exists anymore. Select one of " + subSteps.keySet());

        selectedStep.apply(state, context, workflowService);
    }

    public Map<String, WorkflowStep<?>> getSubSteps() {
        return new LinkedHashMap<>(this.subSteps);
    }
}
