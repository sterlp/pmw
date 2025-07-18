package org.sterl.pmw.model;

import java.io.Serializable;
import java.time.Duration;
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
        super(id, description, connectorLabel);
        this.fn = fn;
        this.subWorkflow = subWorkflow;
        this.delay = delay;
    }

    @Override
    public void apply(WorkflowContext<T> context) {
        SubWorkflowState toStriggerState = this.fn.apply(context.data());
        context.addCommand(new TriggerWorkflowCommand<SubWorkflowState>(subWorkflow, toStriggerState, delay));
    }
}
