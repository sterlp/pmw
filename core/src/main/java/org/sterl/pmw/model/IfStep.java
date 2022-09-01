package org.sterl.pmw.model;

import java.util.Map;

import lombok.Getter;

@Getter
public class IfStep<StateType extends WorkflowState> extends AbstractStep<StateType> {

    private final WorkflowChooseFunction<StateType> chooseFn;
    private final Map<String, WorkflowStep<StateType>> subSteps;

    IfStep(String name, WorkflowChooseFunction<StateType> chooseFn, Map<String, WorkflowStep<StateType>> subSteps) {
        super(name);
        this.chooseFn = chooseFn;
        this.subSteps = subSteps;
    }

    @Override
    public void apply(StateType state, WorkflowContext context) {
        final String stepName = chooseFn.apply(state);
        WorkflowStep<StateType> selectedStep = subSteps.get(stepName);

        if (selectedStep == null) throw new IllegalStateException("No step with name "
                    + stepName + " exists anymore. Select one of " + subSteps.keySet());

        selectedStep.apply(state, context);
    }
}
