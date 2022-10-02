package org.sterl.pmw.component;

import org.sterl.pmw.boundary.WorkflowService;
import org.sterl.pmw.exception.WorkflowException;
import org.sterl.pmw.exception.WorkflowException.WorkflowFailedDoRetryException;
import org.sterl.pmw.exception.WorkflowException.WorkflowFailedNoRetryException;
import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStep;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SimpleWorkflowStepExecutor {

    protected final WorkflowStatusObserver observer;

    /**
     * Runs the next step in the workflow
     *
     * @return {@link WorkflowStep} if a next step is available, otherwise <code>null</code>
     * @throws WorkflowFailedDoRetryException in case of an error, <b>do</b> retry
     * @throws WorkflowFailedNoRetryException in case of an error, <b>no</b> retry
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public WorkflowStep<?> executeNextStep(RunningWorkflowState<?> runningWorkflowState, WorkflowService<?> workflowService) {
        WorkflowStep stepToRun = runningWorkflowState.getCurrentStep();
        if (stepToRun != null) {
            try {
                observer.stepStart(getClass(), runningWorkflowState);
                stepToRun.apply(runningWorkflowState.userState(), runningWorkflowState.internalState(), workflowService);
                observer.stepSuccess(getClass(), runningWorkflowState);

                stepToRun = runningWorkflowState.successStep(stepToRun);
            } catch (Exception e) {
                throw handleWorkflowStepFailed(runningWorkflowState, stepToRun, e);
            }

        }
        return stepToRun;
    }

    private <C extends WorkflowState> WorkflowException handleWorkflowStepFailed(RunningWorkflowState<C> runningWorkflowState, WorkflowStep<C> step, Exception e) {
        boolean willRetry = runningWorkflowState.failStep(step, e);
        WorkflowException result;
        int retryCount = runningWorkflowState.internalState().getLastFailedStepRetryCount();
        if (willRetry) {
            observer.stepFailedRetry(getClass(), runningWorkflowState, e);
            result = new WorkflowException.WorkflowFailedDoRetryException(runningWorkflowState.workflow(), step, e, retryCount);
        } else {
            observer.stepFailed(getClass(), runningWorkflowState, e);
            result = new WorkflowException.WorkflowFailedNoRetryException(runningWorkflowState.workflow(), step, e, retryCount);
        }
        return result;
    }
}
