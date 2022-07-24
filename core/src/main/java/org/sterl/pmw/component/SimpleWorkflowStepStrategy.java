package org.sterl.pmw.component;

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
        while (nextStep != null) {
            log.debug("Selecting step={} on index={}", nextStep.getName(), 
                    context.getInternalWorkflowContext().getCurrentStepIndex());
            try {
                nextStep.apply(context);
                if (w.success(nextStep, context)) {
                    nextStep = w.nextStep(context);
                } else {
                    nextStep = null;
                    logWorkflowEnd(w, context);
                }
            } catch (Exception e) {
                boolean willRetry = w.fail(nextStep, context, e);
                logWorkflowStepFailed(w, nextStep, e, willRetry);
                return willRetry;
            }
        }
        return false;
    }

    private <C extends AbstractWorkflowContext> void logWorkflowStepFailed(Workflow<C> w, WorkflowStep<C> nextStep, Exception e, boolean willRetry) {
        final String msg = "Workflow={} failed in step={} retry={}";
        if (willRetry) {
            log.warn(msg, 
                    w.getName(), nextStep.getName(), willRetry, e);
        } else {
            log.error(msg, 
                    w.getName(), nextStep.getName(), willRetry, e);
        }
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
