package org.sterl.pmw.model;

import java.io.Serializable;
import java.time.Duration;
import java.util.function.Function;

import org.sterl.pmw.WorkflowService;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TriggerWorkflowStep<StateType extends Serializable,
    TriggerWorkflowStateType extends Serializable>
    extends AbstractStep<StateType, StateType> {

    @Getter
    private final Workflow<TriggerWorkflowStateType> toTrigger;
    private final Function<StateType, TriggerWorkflowStateType> fn;
    private final Duration delay;

    TriggerWorkflowStep(String name, String connectorLabel, Workflow<TriggerWorkflowStateType> toTrigger,
        Function<StateType, TriggerWorkflowStateType> fn, Duration delay) {
        super(name, connectorLabel);
        this.fn = fn;
        this.toTrigger = toTrigger;
        this.delay = delay;
    }

    @Override
    public StateType apply(StateType state, WorkflowContext context, WorkflowService<?> workflowService) {
        TriggerWorkflowStateType toStriggerState = this.fn.apply(state);
        workflowService.execute(toTrigger, toStriggerState, delay);
        log.debug("Triggered sub-workflow={}", toTrigger.getName());
        return state;
    }
}
