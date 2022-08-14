package org.sterl.pmw.quartz.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.transaction.support.TransactionTemplate;
import org.sterl.pmw.component.SimpleWorkflowStepStrategy;
import org.sterl.pmw.exception.WorkflowException;
import org.sterl.pmw.model.AbstractWorkflowContext;
import org.sterl.pmw.model.Workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class QuartzWorkflowJob implements Job {
    @NonNull
    private final SimpleWorkflowStepStrategy callStrategy;
    @NonNull
    private final Workflow<? extends AbstractWorkflowContext> w;
    @NonNull
    private final Scheduler scheduler;
    @NonNull
    private final ObjectMapper mapper;
    @NonNull
    private final TransactionTemplate trx;
    
    private static class InternalRetryableJobExeption extends RuntimeException {
        private InternalRetryableJobExeption(Exception e) {
            super(e);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        final AbstractWorkflowContext c = readWorkflowState(context);

        try {
            trx.executeWithoutResult(t -> {
                boolean hasNext = callStrategy.call((Workflow<AbstractWorkflowContext>)w , c);
                if (hasNext) {
                    try {
                        queueNextStepFor(context.getTrigger(), c);
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
            queueNextStepFor(context.getTrigger(), c);
        } catch (Exception e) {
            log.error("workflow={} failed, no retry possible. {}", 
                    context.getTrigger().getKey(), e.getMessage(), e);
        }
    }

    private AbstractWorkflowContext readWorkflowState(JobExecutionContext context) throws JobExecutionException {
        AbstractWorkflowContext c = w.newEmtyContext();
        String state = context.getMergedJobDataMap().getString("_workflowState");
        if (state != null && state.length() > 1) {
            try {
                c = mapper.readValue(state, w.newEmtyContext().getClass());
            } catch (Exception e) {
                throw new RuntimeException(context.getTrigger().getKey() + " failed to parse state: " + state, e);
            }
        }
        return c;
    }
    
    void queueNextStepFor(Trigger trigger, AbstractWorkflowContext c) throws JobExecutionException {
        if (trigger == null || c == null) throw new JobExecutionException(true);
        
        Trigger newTrigger;
        try {
            newTrigger = trigger.getTriggerBuilder()
                    .forJob(trigger.getJobKey())
                    .usingJobData(trigger.getJobDataMap())
                    .usingJobData("_workflowState", mapper.writeValueAsString(c))
                    .startNow()
                    .build();
        } catch (JsonProcessingException e) {
            throw new JobExecutionException(e, true);
        }

        try {
            scheduler.rescheduleJob(trigger.getKey(), newTrigger);
        } catch (SchedulerException e) {
            throw new JobExecutionException(e, true);
        }
    }
}
