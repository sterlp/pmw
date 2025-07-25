package org.sterl.pmw.model;

import java.io.Serializable;

public class SequentialStepFactory<
    C extends StepHolder<T>, T extends Serializable> extends AbstractStepFactory<SequentialStepFactory<C, T>, C, T> {

    private WorkflowFunction<T> function;
    
    public SequentialStepFactory(C context) {
        super(context);
    }
    
    public SequentialStepFactory<C, T> function(WorkflowFunction<T> fn) {
        function = fn;
        return this;
    }
    
    public C build() {
        if (id == null) id = context.nextStepId();
        context.next(new SequentialStep<>(id, description, connectorLabel, function, transactional));
        return context;
    }
}
