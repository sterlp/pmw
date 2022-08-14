package org.sterl.pmw.model;

import java.util.Objects;

public record WorkflowState(Workflow<?> workflow, WorkflowContext userContext, InternalWorkflowState internalState) {
    public WorkflowState {
        Objects.requireNonNull(workflow, "Workflow can't be null.");
        Objects.requireNonNull(userContext, "WorkflowContext can't be null.");
        Objects.requireNonNull(internalState, "InternalWorkflowState can't be null.");
    }
    
    public WorkflowStep<?> nextStep() {
        return workflow.nextStep(internalState);
    }
    
    public WorkflowStep<?> successStep(WorkflowStep<?> step) {
        return ((Workflow)workflow).success(step, internalState);
    }
    
    public boolean failStep(WorkflowStep<?> step, Exception e) {
        return ((Workflow)workflow).fail(step, internalState, e);
    }
}
