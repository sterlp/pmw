package org.sterl.pmw.model;

import java.io.Serializable;
import java.util.Objects;

import lombok.Getter;

@Getter
public class ErrorStep<T extends Serializable> implements WorkflowStep<T> {
    
    private final WorkflowStep<T> step;
    
    public ErrorStep(WorkflowStep<T> step) {
        this.step = step;
        Objects.requireNonNull(step, "ErrorStep needs a nested step");
    }

    @Override
    public String getId() {
        return step.getId();
    }

    @Override
    public String getDescription() {
        return step.getDescription();
    }

    @Override
    public String getConnectorLabel() {
        return step.getConnectorLabel();
    }

    @Override
    public boolean isTransactional() {
        return step.isTransactional();
    }

    @Override
    public void apply(WorkflowContext<T> context) {
        step.apply(context);
    }
}
