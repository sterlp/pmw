package org.sterl.pmw.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.sterl.pmw.WorkflowService;

import lombok.Getter;

@Getter
public class ChooseStep<T extends Serializable, R extends Serializable> extends AbstractStep<T, R> {

    private final WorkflowChooseFunction<T> chooseFn;
    private final Map<String, WorkflowStep<T, R>> subSteps;

    ChooseStep(String name, WorkflowChooseFunction<T> chooseFn, Map<String, WorkflowStep<T, R>> subSteps) {
        super(name, null);
        this.chooseFn = chooseFn;
        this.subSteps = subSteps;
    }

    @Override
    public R apply(T state, WorkflowContext context, WorkflowService<?> workflowService) {
        final String stepName = chooseFn.apply(state);
        WorkflowStep<T, R> selectedStep = subSteps.get(stepName);

        if (selectedStep == null) throw new IllegalStateException("No step with name "
                    + stepName + " exists anymore. Select one of " + subSteps.keySet());

        return selectedStep.apply(state, context, workflowService);
    }

    public Map<String, WorkflowStep<?, ?>> getSubSteps() {
        return new LinkedHashMap<>(this.subSteps);
    }
}
