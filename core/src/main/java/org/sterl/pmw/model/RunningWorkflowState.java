package org.sterl.pmw.model;

import java.util.Objects;

public record RunningWorkflowState<T extends WorkflowState>(Workflow<T> workflow, WorkflowState userContext, InternalWorkflowState internalState) {
    public RunningWorkflowState {
        Objects.requireNonNull(workflow, "Workflow can't be null.");
        Objects.requireNonNull(userContext, "WorkflowState can't be null.");
        Objects.requireNonNull(internalState, "InternalWorkflowState can't be null.");
    }
    
    public WorkflowStep<T> nextStep() {
        return workflow.nextStep(internalState);
    }
    
    public WorkflowStep<T> successStep(WorkflowStep<T> step) {
        return workflow.success(step, internalState);
    }
    
    public boolean failStep(WorkflowStep<T> step, Exception e) {
        return workflow.fail(step, internalState, e);
    }
}
