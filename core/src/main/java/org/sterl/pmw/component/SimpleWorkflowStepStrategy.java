package org.sterl.pmw.component;

import org.sterl.pmw.exception.WorkflowException;
import org.sterl.pmw.model.WorkflowContext;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStep;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleWorkflowStepStrategy {

    /**
     * Runs the next step in the workflow
     * @return <code>true</code> if a retry or next step should run, otherwise <code>false</code>
     */
    public boolean call(WorkflowState workflowState) {
        WorkflowStep nextStep = workflowState.nextStep();
        logWorkflowStart(workflowState);
        if (nextStep != null) {
            log.debug("Selecting step={} on workflow={}", nextStep.getName(), 
                    workflowState.workflow().getName());
            try {
                nextStep.apply(workflowState.userContext());
                nextStep = workflowState.successStep(nextStep);

                if (nextStep == null) logWorkflowEnd(workflowState);
            } catch (Exception e) {
                throw logWorkflowStepFailed(workflowState, nextStep, e);
            }
        }
        return nextStep != null;
    }

    private <C extends WorkflowContext> WorkflowException logWorkflowStepFailed(WorkflowState workflowState, WorkflowStep<?> step, Exception e) {
        boolean willRetry = workflowState.failStep(step, e);
        WorkflowException result;
        int retryCount = workflowState.internalState().getLastFailedStepRetryCount();
        if (willRetry) {
            result = new WorkflowException.WorkflowFailedDoRetryException(workflowState.workflow(), step, e, retryCount);
            log.warn("{} retryCount={}", e.getMessage(), retryCount, e);
        } else {
            result = new WorkflowException.WorkflowFailedNoRetryException(workflowState.workflow(), step, e, retryCount);
            log.error("{} retryCount={}", e.getMessage(), retryCount, e);
        }
        return result;
    }

    private <C extends WorkflowContext> void logWorkflowEnd(WorkflowState workflowState) {
        log.info("workflow={} success durationMs={} at={}.",
                workflowState.workflow().getName(),
                workflowState.internalState().workflowRunDuration().toMillis(), 
                workflowState.internalState().getWorkflowEnd());
    }

    private <C extends WorkflowContext> void logWorkflowStart(WorkflowState workflowState) {
        if (workflowState.internalState().isFirstWorkflowStep()) {
            log.info("Starting workflow={} at={}", workflowState.workflow().getName(), workflowState.internalState().getWorkflowStart());
        }
    }
}
