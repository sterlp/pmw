package org.sterl.pmw.component;

import org.sterl.pmw.exception.WorkflowException;
import org.sterl.pmw.model.AbstractWorkflowContext;
import org.sterl.pmw.model.AbstractWorkflowContext.InternalWorkflowContext;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowStep;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleWorkflowStepStrategy {

    /**
     * Runs the next step in the workflow
     * @return <code>true</code> if a retry or next step should run, otherwise <code>false</code>
     */
    public <C extends AbstractWorkflowContext> boolean call(Workflow<C> w, C context) {
        WorkflowStep<C> nextStep = w.nextStep(context);
        logWorkflowStart(w, context);
        if (nextStep != null) {
            log.debug("Selecting step={} on workflow={}", nextStep.getName(), 
                    w.getName());
            try {
                nextStep.apply(context);
                if (w.success(nextStep, context)) {
                    nextStep = w.nextStep(context);
                } else {
                    nextStep = null;
                    logWorkflowEnd(w, context);
                }
            } catch (Exception e) {
                throw logWorkflowStepFailed(w, nextStep, context, e);
            }
        }
        return w.nextStep(context) != null;
    }

    private <C extends AbstractWorkflowContext> WorkflowException logWorkflowStepFailed(Workflow<C> w, WorkflowStep<C> nextStep, C context, Exception e) {
        boolean willRetry = w.fail(nextStep, context, e);
        WorkflowException result;
        int retryCount = context.getInternalWorkflowContext().getLastFailedStepRetryCount();
        if (willRetry) {
            result = new WorkflowException.WorkflowFailedDoRetryException(w, nextStep, e, retryCount);
            log.warn("{} retryCount={}", e.getMessage(), retryCount, e);
        } else {
            result = new WorkflowException.WorkflowFailedNoRetryException(w, nextStep, e, retryCount);
            log.error("{} retryCount={}", e.getMessage(), retryCount, e);
        }
        return result;
    }

    private <C extends AbstractWorkflowContext> void logWorkflowEnd(Workflow<C> w, C c) {
        InternalWorkflowContext state = c.getInternalWorkflowContext();
        log.info("workflow={} success durationMs={} at={}.",
                w.getName(),
                state.workflowRunDuration().toMillis(), state.getWorkflowEnd());
    }

    private <C extends AbstractWorkflowContext> void logWorkflowStart(Workflow<C> w, C c) {
        InternalWorkflowContext state = c.getInternalWorkflowContext();
        if (state.isFirstWorkflowStep()) {
            log.info("Starting workflow={} at={}", w.getName(), state.getWorkflowStart());
        }
    }
}
