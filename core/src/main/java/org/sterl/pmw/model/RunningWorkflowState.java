package org.sterl.pmw.model;

import java.util.Objects;

public record RunningWorkflowState<T extends WorkflowState>(
        WorkflowId workflowId,
        Workflow<T> workflow, WorkflowState userState, InternalWorkflowState internalState) {
    public RunningWorkflowState {
        Objects.requireNonNull(workflowId, "Workflow can't be null.");
        Objects.requireNonNull(workflow, "Workflow can't be null.");
        Objects.requireNonNull(userState, "WorkflowState can't be null.");
        Objects.requireNonNull(internalState, "InternalWorkflowState can't be null.");
    }

    public WorkflowStep<T> getCurrentStep() {
        return workflow.getCurrentStep(internalState);
    }

    public WorkflowStep<T> successStep(WorkflowStep<T> step) {
        return workflow.success(step, internalState);
    }

    /**
     * Increments the fail counter for the given step and set the {@link WorkflowStatus} to {@link WorkflowStatus#FAILED}
     * if the max retry count is exceeded.
     *
     * @return <code>true</code> retry should be attempted, otherwise <code>false</code>
     */
    public boolean failStep(WorkflowStep<T> step, Exception e) {
        return workflow.fail(step, internalState, e);
    }

    public boolean isNextStepDelayed() {
        return isNotFailed() && isNotCanceled() && internalState.hasDelay();
    }
    public boolean hasNoDelay() {
        return !internalState.hasDelay();
    }

    public boolean isComplete() {
        return internalState.getWorkflowStatus() == WorkflowStatus.COMPLETE;
    }


    public boolean isCanceled() {
        return internalState.getWorkflowStatus() == WorkflowStatus.CANCELED;
    }
    public boolean isNotCanceled() {
        return internalState.getWorkflowStatus() != WorkflowStatus.CANCELED;
    }

    public boolean isNotFailed() {
        return internalState.getWorkflowStatus() != WorkflowStatus.FAILED;
    }

    public boolean isNextStepReady() {
        return hasNoDelay() && isNotCanceled() && isNotFailed();
    }

    @Override
    public String toString() {
        return "RunningWorkflowState[Workflow=" + workflow.getName()
            + ", " + workflowId + " status=" + internalState.getWorkflowStatus() + "]";
    }

    /**
     * @return <code>true</code> if complete or canceled
     */
    public boolean isFinished() {
        return isCanceled() || isComplete();
    }
}
