package org.sterl.pmw.model;

import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

import org.sterl.pmw.command.TriggerWorkflowCommand;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TriggerWorkflowStep<T extends Serializable,
    SubWorkflowState extends Serializable> extends AbstractStep<T> {

    @Getter
    private final Workflow<SubWorkflowState> subWorkflow;
    private final Function<T, SubWorkflowState> fn;
    @Getter
    private final Duration delay;

    TriggerWorkflowStep(String id, String description, String connectorLabel, Workflow<SubWorkflowState> subWorkflow,
        Function<T, SubWorkflowState> fn, Duration delay) {
        super(id, description, connectorLabel, true);

        this.fn = fn;
        this.subWorkflow = subWorkflow;
        this.delay = delay;
        
        Objects.requireNonNull(subWorkflow, "Workflow to trigger required");
    }

    @Override
    public void apply(WorkflowContext<T> context) {
        
        SubWorkflowState toStriggerState = null;
        
        if (this.fn == null) {
            var subStateType = subWorkflow.newContext().getClass();
            if (subStateType.isInstance(context.data())) {
                toStriggerState = (SubWorkflowState)subStateType.cast(context.data());
            }
        } else {
            toStriggerState = this.fn.apply(context.data());
        }
        context.addCommand(new TriggerWorkflowCommand<SubWorkflowState>(subWorkflow, toStriggerState, delay));
    }
}
