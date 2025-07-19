package org.sterl.pmw.model;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Function;

/**
 * Allows the selection of an task.
 */
public class ChooseFactory<C extends StepHolder<T>, T extends Serializable> 
    extends AbstractStepFactory<ChooseFactory<C, T>,C, T>
    implements StepHolder<T> {

    private WorkflowChooseFunction<T> chooseFn;
    private StepContainer<T> steps = new StepContainer<>();

    public ChooseFactory(C context) {
        super(context);
    }
    
    public ChooseFactory<C, T> chooseFn(WorkflowChooseFunction<T> value) {
        chooseFn = value;
        return this;
    }

    public ChooseFactory<C, T> ifSelected(String stepId, WorkflowFunction<T> fn) {
        next(new SequentialStep<>(stepId, fn));
        return this;
    }
    
    public SequentialStepFactory<ChooseFactory<C, T>, T> ifSelected(String id) {
        return new SequentialStepFactory<>(this).id(id);
    }
    
    public <SubT extends Serializable>
        TriggerWorkflowStepFactory<ChooseFactory<C, T>, T, SubT> ifTrigger(String id,
            Workflow<SubT> subWorkflow, Function<T, SubT> fn) {
      
        return new TriggerWorkflowStepFactory<>(this, subWorkflow)
                     .function(fn)
                     .id(id);
    }
    
    public <SubT extends Serializable> TriggerWorkflowStepFactory<ChooseFactory<C, T>, T, SubT> ifTrigger(
            String id, Workflow<SubT> subWorkflow) {
        return new TriggerWorkflowStepFactory<>(this, subWorkflow).id(id);
    }

    public SequentialStepFactory<ChooseFactory<C, T>, T> ifSelected() {
        return new SequentialStepFactory<>(this);
    }

    public C build() {
        if (id == null) id = nextStepId();
        if (description == null) description = "Choose from " + steps.getSteps().size();
        context.next(new ChooseStep<>(id, description, connectorLabel, transactional, chooseFn, steps.getSteps()));
        return context;
    }

    @Override
    public ChooseFactory<C, T> next(WorkflowStep<T> s) {
        steps.next(s);
        return this;
    }

    @Override
    public Map<String, WorkflowStep<T>> steps() {
        return steps.getSteps();
    }

    @Override
    public String nextStepId() {
        return context.nextStepId();
    }
}
