package org.sterl.pmw.component;

import org.sterl.pmw.boundary.WorkflowService;
import org.sterl.pmw.exception.WorkflowException;
import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStep;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleWorkflowStepStrategy {

    /**
     * Runs the next step in the workflow
     * @return {@link WorkflowStep} if a next step is available, otherwise <code>null</code>
     * @throws WorkflowException in case of an error
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public WorkflowStep executeNextStep(RunningWorkflowState<?> runningWorkflowState, WorkflowService<?> workflowService) {
        WorkflowStep nextStep = runningWorkflowState.nextStep();
        logWorkflowStart(runningWorkflowState);
        if (nextStep != null) {
            log.debug("Selecting step={} on workflow={}", nextStep.getName(),
                    runningWorkflowState.workflow().getName());
            try {
                nextStep.apply(runningWorkflowState.userState(), runningWorkflowState.internalState(), workflowService);
                nextStep = runningWorkflowState.successStep(nextStep);

                if (nextStep == null) logWorkflowEnd(runningWorkflowState);
            } catch (Exception e) {
                throw logWorkflowStepFailed(runningWorkflowState, nextStep, e);
            }

        }
        return nextStep;
    }

    private <C extends WorkflowState> WorkflowException logWorkflowStepFailed(RunningWorkflowState<C> runningWorkflowState, WorkflowStep<C> step, Exception e) {
        boolean willRetry = runningWorkflowState.failStep(step, e);
        WorkflowException result;
        int retryCount = runningWorkflowState.internalState().getLastFailedStepRetryCount();
        if (willRetry) {
            result = new WorkflowException.WorkflowFailedDoRetryException(runningWorkflowState.workflow(), step, e, retryCount);
            log.warn("{} retryCount={}", e.getMessage(), retryCount, e);
        } else {
            result = new WorkflowException.WorkflowFailedNoRetryException(runningWorkflowState.workflow(), step, e, retryCount);
            log.error("{} retryCount={}", e.getMessage(), retryCount, e);
        }
        return result;
    }

    private <C extends WorkflowState> void logWorkflowEnd(RunningWorkflowState<C> runningWorkflowState) {
        log.info("workflow={} success durationMs={} at={}.",
                runningWorkflowState.workflow().getName(),
                runningWorkflowState.internalState().workflowRunDuration().toMillis(),
                runningWorkflowState.internalState().getWorkflowEndTime());
    }

    private <C extends WorkflowState> void logWorkflowStart(RunningWorkflowState<C> runningWorkflowState) {
        if (runningWorkflowState.internalState().isFirstWorkflowStep()) {
            log.info("Starting workflow={} at={}",
                    runningWorkflowState.workflow().getName(),
                    runningWorkflowState.internalState().getWorkflowStartTime());
        }
    }
}
