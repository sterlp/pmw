package org.sterl.pmw.component;

import org.sterl.pmw.exception.WorkflowException;
import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStep;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleWorkflowStepStrategy {

    /**
     * Runs the next step in the workflow
     * @return <code>true</code> if a retry or next step should run, otherwise <code>false</code>
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public boolean call(RunningWorkflowState runningWorkflowState) {
        WorkflowStep nextStep = runningWorkflowState.nextStep();
        logWorkflowStart(runningWorkflowState);
        if (nextStep != null) {
            log.debug("Selecting step={} on workflow={}", nextStep.getName(), 
                    runningWorkflowState.workflow().getName());
            try {
                nextStep.apply(runningWorkflowState.userContext(), runningWorkflowState.internalState());
                nextStep = runningWorkflowState.successStep(nextStep);

                if (nextStep == null) logWorkflowEnd(runningWorkflowState);
            } catch (Exception e) {
                throw logWorkflowStepFailed(runningWorkflowState, nextStep, e);
            }
        }
        return nextStep != null;
    }

    private <C extends WorkflowState> WorkflowException logWorkflowStepFailed(RunningWorkflowState runningWorkflowState, WorkflowStep<?> step, Exception e) {
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

    private <C extends WorkflowState> void logWorkflowEnd(RunningWorkflowState runningWorkflowState) {
        log.info("workflow={} success durationMs={} at={}.",
                runningWorkflowState.workflow().getName(),
                runningWorkflowState.internalState().workflowRunDuration().toMillis(), 
                runningWorkflowState.internalState().getWorkflowEndTime());
    }

    private <C extends WorkflowState> void logWorkflowStart(RunningWorkflowState runningWorkflowState) {
        if (runningWorkflowState.internalState().isFirstWorkflowStep()) {
            log.info("Starting workflow={} at={}", 
                    runningWorkflowState.workflow().getName(), 
                    runningWorkflowState.internalState().getWorkflowStartTime());
        }
    }
}
