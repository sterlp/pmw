package org.sterl.pmw.quartz.boundary;

import java.util.HashMap;
import java.util.Map;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.sterl.pmw.boundary.WorkflowService;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.AbstractWorkflowContext;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.quartz.job.QuartzWorkflowJob;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QuartzWorkflowService implements WorkflowService<JobDetail> {

    @NonNull
    private final Scheduler scheduler;
    @NonNull
    private final WorkflowRepository workflowRepository;
    private final Map<String, JobDetail> workflowJobs = new HashMap<>();

    public <T extends AbstractWorkflowContext> String execute(Workflow<T> w, T c) {
        JobDetail job = workflowJobs.get(w.getName());
        if (job == null) throw new IllegalStateException(
                w.getName() + " not registered, register the workflowJobs first.");

        try {
            Trigger t = TriggerBuilder.newTrigger()
                    .forJob(job)
                    .startNow()
                    .build();
            
            scheduler.scheduleJob(t);
            return t.getKey().getName();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public <T extends AbstractWorkflowContext> String execute(Workflow<T> w) {
        return execute(w, w.newEmtyContext());
    }

    @Override
    public <T extends AbstractWorkflowContext> JobDetail register(Workflow<T> w) {
        JobDetail job = JobBuilder.newJob(QuartzWorkflowJob.class)
                .withIdentity(w.getName(), "pmw")
                .storeDurably()
                .build();

        try {
            workflowRepository.registerUnique(w);
            scheduler.addJob(job, true);
            workflowJobs.put(w.getName(), job);
            return job;
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    

    @Override
    public void clearAllWorkflows() {
        for (JobDetail d : workflowJobs.values()) {
            try {
                var triggerKeys = scheduler.getTriggersOfJob(d.getKey())
                        .stream().map(t -> t.getKey())
                        .toList();
                scheduler.unscheduleJobs(triggerKeys);
            } catch (SchedulerException e) {
                throw new RuntimeException("Failed to clear jobs", e);
            }
            
        }
    }
}
