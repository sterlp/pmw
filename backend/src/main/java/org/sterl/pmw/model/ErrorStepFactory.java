package org.sterl.pmw.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class ErrorStepFactory<C extends StepHolder<C, T>, T extends Serializable>
    implements StepHolder<ErrorStepFactory<C, T>, T> {

    private final C context;
    private WorkflowStep<T> step;

    public ErrorStepFactory(C context) {
        this.context = context;
        Objects.requireNonNull(context, "Context cannot be null");
    }
    
    public SequentialStepFactory<ErrorStepFactory<C, T>, T> next(String id) {
        return new SequentialStepFactory<>(this).id(id);
    }
    public <R extends Serializable> TriggerWorkflowStepFactory<ErrorStepFactory<C, T>, T, R> forkWorkflow(Workflow<R> workflow) {
        return new TriggerWorkflowStepFactory<>(this, workflow);
    }
    public C build() {
        context.next(new ErrorStep<>(step));
        return context;
    }

    @Override
    public ErrorStepFactory<C, T> next(WorkflowStep<T> s) {
        step = s;
        return this;
    }

    @Override
    public Map<String, WorkflowStep<T>> steps() {
        return Collections.emptyMap();
    }

    @Override
    public String nextStepId() {
        return context.nextStepId();
    }
}
