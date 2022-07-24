package org.sterl.pmw.quartz.job;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.sterl.pmw.component.SimpleWorkflowStepStrategy;
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
    

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobData = context.getMergedJobDataMap();
        AbstractWorkflowContext c = readWorkflowState(jobData);
        
        boolean hasNext = callStrategy.call((Workflow)w , c);
        
        if (hasNext) {
            try {
                queueNextStepFor(context.getTrigger(), c);
            } catch (SchedulerException e) {
                throw new JobExecutionException(e, true);
            }
        }
    }

    private AbstractWorkflowContext readWorkflowState(JobDataMap jobData) throws JobExecutionException {
        AbstractWorkflowContext c = w.newEmtyContext();
        String state = jobData.getString("_workflowState");
        if (state != null && state.length() > 3) {
            try {
                c = mapper.readValue(state, w.newEmtyContext().getClass());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return c;
    }
    
    void queueNextStepFor(Trigger trigger, AbstractWorkflowContext c) throws SchedulerException {
        Trigger newTrigger;
        try {
            newTrigger = TriggerBuilder.newTrigger()
                    .forJob(trigger.getJobKey())
                    .usingJobData(trigger.getJobDataMap())
                    .usingJobData("_workflowState", mapper.writeValueAsString(c))
                    .startNow()
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        log.debug("Tiggering step={} workflow={} - oldKey={} newKey={}", 
                w.getName(), c.getInternalWorkflowContext().getCurrentStepIndex(), trigger.getKey(), newTrigger.getKey());
        scheduler.rescheduleJob(trigger.getKey(), newTrigger);
    }
}
