package org.sterl.pmw.quartz.boundary;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.quartz.DateBuilder;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.sterl.pmw.boundary.WorkflowService;
import org.sterl.pmw.component.SerializationUtil;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.InternalWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStatus;
import org.sterl.pmw.quartz.component.WorkflowStateParserComponent;
import org.sterl.pmw.quartz.job.QuartzWorkflowJob;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QuartzWorkflowService implements WorkflowService<JobDetail> {

    private final Scheduler scheduler;
    private final WorkflowRepository workflowRepository;
    private final Map<String, JobDetail> workflowJobs = new HashMap<>();
    private final WorkflowStateParserComponent workflowStateParser;

    public QuartzWorkflowService(@NonNull Scheduler scheduler, @NonNull WorkflowRepository workflowRepository,
            ObjectMapper mapper) {
        super();
        this.scheduler = scheduler;
        this.workflowRepository = workflowRepository;
        this.workflowStateParser = new WorkflowStateParserComponent(mapper);

        log.info("Workflows initialized, {} workflows deployed.", workflowRepository.getWorkflowNames().size());
    }

    @Override
    public <T extends WorkflowState> String execute(Workflow<T> w) {
        return execute(w, w.newEmtyContext());
    }

    @Override
    public <T extends WorkflowState> JobDetail register(Workflow<T> w) {
        JobDetail job = JobBuilder.newJob(QuartzWorkflowJob.class)
                .withIdentity(w.getName(), "pmw")
                .storeDurably()
                .build();

        try {
            workflowRepository.registerUnique((Workflow<WorkflowState>) w);
            scheduler.addJob(job, true);
            workflowJobs.put(w.getName(), job);
            return job;
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clearAllWorkflows() {
        this.workflowRepository.clear();
        for (JobDetail d : workflowJobs.values()) {
            try {
                var triggerKeys = scheduler.getTriggersOfJob(d.getKey())
                        .stream().map(t -> t.getKey())
                        .collect(Collectors.toList());
                scheduler.unscheduleJobs(triggerKeys);
            } catch (SchedulerException e) {
                throw new RuntimeException("Failed to clear jobs", e);
            }

        }
    }

    @SuppressWarnings({ "rawtypes" })
    @Override
    public String execute(String workflowName) {
        Workflow w = workflowRepository.getWorkflow(workflowName);
        return execute(workflowName, w.newEmtyContext());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public String execute(String workflowName, WorkflowState state) {
        Workflow w = workflowRepository.getWorkflow(workflowName);
        SerializationUtil.verifyStateType(w, state);
        return execute(w, state);
    }
    @Override
    public <T extends WorkflowState> String execute(Workflow<T> w, T state) {
        return execute(w, state, Duration.ZERO);
    }
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public String execute(String workflowName, WorkflowState state, Duration delay) {
        Workflow w = workflowRepository.getWorkflow(workflowName);
        SerializationUtil.verifyStateType(w, state);
        return execute(w, state, delay);
    }

    @Override
    public <T extends WorkflowState> String execute(Workflow<T> w, T state, Duration delay) {
        JobDetail job = workflowJobs.get(w.getName());
        if (job == null) throw new IllegalStateException(
                w.getName() + " not registered, register the workflowJobs first.");

        try {
            final TriggerBuilder<Trigger> t = TriggerBuilder.newTrigger().forJob(job);
            
            
            setWorkflowDelayStatusAndSate(t, WorkflowStatus.PENDING, state, new InternalWorkflowState(delay));

            final Trigger trigger = t.build();
            scheduler.scheduleJob(trigger);
            return trigger.getKey().getName();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public WorkflowStatus status(String workflowId) {
        try {
            final Trigger workflowTrigger = scheduler.getTrigger(TriggerKey.triggerKey(workflowId));
            WorkflowStatus result;
            if (workflowTrigger == null || workflowTrigger.getEndTime() != null) {
                result = WorkflowStatus.COMPLETE;
            } else {
                if (workflowTrigger.getStartTime() == null) {
                    result = WorkflowStatus.PENDING;
                } else {
                    final WorkflowStatus ws = workflowStateParser.getWorkflowStatus(workflowTrigger.getJobDataMap());
                    if (ws == WorkflowStatus.SLEEPING) result = ws;
                    else result = WorkflowStatus.RUNNING;
                }
            }
            return result;
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int workflowCount() {
        return workflowRepository.workflowCount();
    }

    public Optional<Workflow<? extends WorkflowState>> findWorkflow(String name) {
        return this.workflowRepository.findWorkflow(name);
    }

    /**
     * RescheduleJob a trigger of a already running workflow.
     * 
     * @param trigger required trigger to reschedule with the reference to the workflow
     * @param internalState the required current internal state
     * @param userState optional user state to set into the trigger, maybe <code>null</code>
     * @throws JobExecutionException if the trigger or {@link InternalWorkflowState} is <code>null</code>
     */
    public void rescheduleTrigger(Trigger trigger, 
            InternalWorkflowState internalState,
            WorkflowState userState) throws JobExecutionException {
        if (trigger == null || internalState == null) throw new JobExecutionException(true);

        TriggerBuilder<? extends Trigger>  newTrigger;
        try {
            newTrigger = trigger.getTriggerBuilder()
                    .forJob(trigger.getJobKey())
                    .usingJobData(trigger.getJobDataMap());

            setWorkflowDelayStatusAndSate(newTrigger, WorkflowStatus.RUNNING, userState, internalState);

        } catch (JsonProcessingException e) {
            throw new JobExecutionException(e, true);
        }


        try {
            boolean notSchduled = scheduler.rescheduleJob(trigger.getKey(), newTrigger.build()) == null;
            if (notSchduled) {
                scheduler.scheduleJob(newTrigger.build());
            }
        } catch (SchedulerException e) {
            throw new JobExecutionException(e, true);
        }
    }

    private void setWorkflowDelayStatusAndSate(
            TriggerBuilder<? extends Trigger> trigger, 
            WorkflowStatus desiredWorkflowStatus,
            WorkflowState userState,
            InternalWorkflowState internalState) throws JsonProcessingException {
        
        Duration delay;
        if (internalState == null) delay = Duration.ZERO;
        else delay = internalState.consumeDelay();
        
        workflowStateParser.setInternalState(trigger, internalState);
        workflowStateParser.setUserState(trigger, userState);

        if (delay.toMillis() > 0L) {
            trigger.startAt(DateBuilder.futureDate((int)delay.toMillis(), IntervalUnit.MILLISECOND));
            workflowStateParser.setWorkflowStatus(trigger, WorkflowStatus.SLEEPING);
        } else {
            trigger.startNow();
            workflowStateParser.setWorkflowStatus(trigger, desiredWorkflowStatus);
        }
    }
}
