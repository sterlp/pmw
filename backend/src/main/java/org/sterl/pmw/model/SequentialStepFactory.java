package org.sterl.pmw.model;

import java.io.Serializable;

public class SequentialStepFactory<C extends StepHolder<C, T>, T extends Serializable>
        extends AbstractStepFactory<SequentialStepFactory<C, T>, C, T> {

    protected WorkflowFunction<T> function;

    public SequentialStepFactory(C context) {
        super(context);
    }

    public SequentialStepFactory<C, T> function(WorkflowFunction<T> fn) {
        this.function = fn;
        return this;
    }

    public SequentialStep<T> buildStep() {
        return new SequentialStep<T>(id, description, connectorLabel, function, transactional);
    }
}
