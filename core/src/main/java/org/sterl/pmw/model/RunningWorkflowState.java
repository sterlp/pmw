package org.sterl.pmw.model;

import java.util.Objects;

public record RunningWorkflowState<T extends WorkflowState>(Workflow<T> workflow, WorkflowState userState, InternalWorkflowState internalState) {
    public RunningWorkflowState {
        Objects.requireNonNull(workflow, "Workflow can't be null.");
        Objects.requireNonNull(userState, "WorkflowState can't be null.");
        Objects.requireNonNull(internalState, "InternalWorkflowState can't be null.");
    }

    public WorkflowStep<T> nextStep() {
        return workflow.nextStep(internalState);
    }

    @Deprecated // should be in the service
    public WorkflowStep<T> successStep(WorkflowStep<T> step) {
        return workflow.success(step, internalState);
    }

    /**
     * Increments the fail counter for the given step and set the {@link WorkflowStatus} to {@link WorkflowStatus#FAILED}
     * if the max retry count is exceeded.
     * 
     * @return <code>true</code> retry should be attempted, otherwise <code>false</code>
     */
    @Deprecated // should be in the service
    public boolean failStep(WorkflowStep<T> step, Exception e) {
        return workflow.fail(step, internalState, e);
    }

    /**
     * Should this workflow still run?
     */
    public boolean isAlive() {
        return isNotFailed() && isNotCanceled();
    }
    public boolean hasNoDelay() {
        return !internalState.hasDelay();
    }

    public boolean isCanceled() {
        return internalState.getStatus() == WorkflowStatus.CANCELED;
    }
    public boolean isNotCanceled() {
        return internalState.getStatus() != WorkflowStatus.CANCELED;
    }
    
    public boolean isNotFailed() {
        return internalState.getStatus() != WorkflowStatus.FAILED;
    }
    
    public boolean isNextStepReady() {
        return hasNoDelay() && isNotCanceled() && isNotFailed();
    }
}
