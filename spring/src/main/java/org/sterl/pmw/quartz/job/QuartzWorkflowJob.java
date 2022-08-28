package org.sterl.pmw.quartz.job;

import java.time.Duration;
import java.util.Optional;

import org.quartz.DateBuilder;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import org.sterl.pmw.component.SimpleWorkflowStepStrategy;
import org.sterl.pmw.exception.WorkflowException;
import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowStatus;
import org.sterl.pmw.quartz.component.WorkflowStateParserComponent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QuartzWorkflowJob implements Job {
    
    @NonNull
    private final SimpleWorkflowStepStrategy callStrategy;
    @NonNull
    private final Workflow<?> w;
    @NonNull
    private final Scheduler scheduler;
    @NonNull
    private final ObjectMapper mapper;
    @NonNull
    private final TransactionTemplate trx;
    
    private final WorkflowStateParserComponent workflowStateParser;
    
    public QuartzWorkflowJob(@NonNull SimpleWorkflowStepStrategy callStrategy, @NonNull Workflow<?> w,
            @NonNull Scheduler scheduler, @NonNull ObjectMapper mapper, @NonNull TransactionTemplate trx) {
        super();
        this.callStrategy = callStrategy;
        this.w = w;
        this.scheduler = scheduler;
        this.mapper = mapper;
        this.trx = trx;
        this.workflowStateParser = new WorkflowStateParserComponent(mapper);
    }

    private static class InternalRetryableJobExeption extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private InternalRetryableJobExeption(Exception e) {
            super(e);
        }
    }
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        final RunningWorkflowState runningWorkflowState = workflowStateParser.readWorkflowState(w, context);

        try {
            trx.executeWithoutResult(t -> {
                boolean hasNext = callStrategy.call(runningWorkflowState);
                if (hasNext) {
                    try {
                        queueNextStepFor(context.getTrigger(), runningWorkflowState);
                    } catch (SchedulerException e) {
                        t.setRollbackOnly();
                        throw new InternalRetryableJobExeption(e);
                    }
                }
            });
        } catch (InternalRetryableJobExeption e) {
            // something internally went wrong, rollback and retry
            Throwable cause = e.getCause();
            if (cause instanceof JobExecutionException jee) throw jee;
            else throw new JobExecutionException(cause, true);

        } catch (WorkflowException.WorkflowFailedDoRetryException retryE) {
            queueNextStepFor(context.getTrigger(), runningWorkflowState);
        } catch (Exception e) {
            log.error("workflow={} failed, no retry possible. {}", 
                    context.getTrigger().getKey(), e.getMessage(), e);
        }
    }

    void queueNextStepFor(Trigger trigger, RunningWorkflowState c) throws JobExecutionException {
        if (trigger == null || c == null) throw new JobExecutionException(true);
        
        TriggerBuilder<? extends Trigger>  newTrigger;
        try {
            newTrigger = trigger.getTriggerBuilder()
                    .forJob(trigger.getJobKey())
                    .usingJobData(trigger.getJobDataMap());
            
            applyWorkflowDelay(c, newTrigger);
            // TODO only set internal state in case of error
            workflowStateParser.setState(newTrigger, c);
        } catch (JsonProcessingException e) {
            throw new JobExecutionException(e, true);
        }


        try {
            scheduler.rescheduleJob(trigger.getKey(), newTrigger.build());
        } catch (SchedulerException e) {
            throw new JobExecutionException(e, true);
        }
    }

    private void applyWorkflowDelay(RunningWorkflowState c, TriggerBuilder<? extends Trigger> newTrigger) {
        final Optional<Duration> delay = c.internalState().clearDelay();

        if (delay.isPresent() && delay.get().toMillis() > 0L) {
            newTrigger.startAt(DateBuilder.futureDate((int)delay.get().toMillis(), IntervalUnit.MILLISECOND));
            workflowStateParser.setWorkflowStatus(newTrigger, WorkflowStatus.SLEEPING);
        } else {
            newTrigger.startNow();
            workflowStateParser.setWorkflowStatus(newTrigger, WorkflowStatus.RUNNING);
        }
    }
}
