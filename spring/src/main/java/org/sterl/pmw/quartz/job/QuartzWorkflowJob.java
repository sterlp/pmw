package org.sterl.pmw.quartz.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.springframework.transaction.support.TransactionTemplate;
import org.sterl.pmw.component.SimpleWorkflowStepExecutor;
import org.sterl.pmw.component.WorkflowStatusObserver;
import org.sterl.pmw.exception.WorkflowException;
import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowStep;
import org.sterl.pmw.quartz.boundary.QuartzWorkflowService;
import org.sterl.pmw.quartz.component.WorkflowStateParserComponent;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QuartzWorkflowJob implements Job {

    @NonNull
    private final SimpleWorkflowStepExecutor callStrategy;
    @NonNull
    private final QuartzWorkflowService workflowService;
    @NonNull
    private final Workflow<?> workflow;
    @NonNull
    private final TransactionTemplate trx;
    @NonNull
    private final WorkflowStateParserComponent workflowStateParser;
    @NonNull
    private final WorkflowStatusObserver workflowStatusObserver;

    private static class InternalRetryableJobExeption extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private InternalRetryableJobExeption(Exception e) {
            super(e);
        }
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        final RunningWorkflowState<?> runningWorkflowState = workflowStateParser.readWorkflowState(workflow, context);
        
        if (runningWorkflowState.internalState().isFirstWorkflowStep()) {
            workflowStatusObserver.workflowStart(getClass(), runningWorkflowState);
        }

        try {
            trx.executeWithoutResult(t -> {
                final WorkflowStep<?> nextStep = callStrategy.executeNextStep(runningWorkflowState, workflowService);
                if (nextStep != null && runningWorkflowState.isNotCanceled()) {
                    try {
                        workflowService.rescheduleTrigger(context.getTrigger(),
                                runningWorkflowState.internalState(),
                                runningWorkflowState.userState());

                    } catch (SchedulerException e) {
                        t.setRollbackOnly();
                        throw new InternalRetryableJobExeption(e);
                    }
                } else {
                    workflowStatusObserver.workflowSuccess(getClass(), runningWorkflowState);
                }
            });
        } catch (InternalRetryableJobExeption e) {
            workflowStatusObserver.stepFailedRetry(getClass(), runningWorkflowState, e);
            // something internally went wrong, rollback and retry
            Throwable cause = e.getCause();
            if (cause instanceof JobExecutionException jee) throw jee;
            else throw new JobExecutionException(cause, true);

        } catch (WorkflowException.WorkflowFailedDoRetryException retryE) {

            workflowService.rescheduleTrigger(context.getTrigger(),
                    runningWorkflowState.internalState(),
                    null);

        } catch (Exception e) {
            workflowStatusObserver.workflowFailed(getClass(), runningWorkflowState, e);
        }
    }
}
