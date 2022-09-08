package org.sterl.pmw.quartz.job;

import java.util.Optional;

import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.transaction.support.TransactionTemplate;
import org.sterl.pmw.component.SimpleWorkflowStepStrategy;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.quartz.boundary.QuartzWorkflowService;
import org.sterl.pmw.quartz.component.WorkflowStateParserComponent;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class QuartzWorkflowJobFactory implements JobFactory {

    @NonNull
    private final SimpleWorkflowStepStrategy strategy;
    @NonNull
    private final QuartzWorkflowService workflowService;
    @NonNull
    private final ObjectMapper mapper;
    @NonNull
    private final TransactionTemplate trx;

    private final JobFactory delegate;

    @Override
    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
        String name = bundle.getJobDetail().getKey().getName();
        final Optional<Workflow<? extends WorkflowState>> w = workflowService.findWorkflow(name);

        log.debug("{} results in {}", name, w);
        if (w.isEmpty() && delegate == null) {
            throw new IllegalStateException("No workflow with the name " + name);
        } else if (w.isEmpty() && delegate != null) {
            return delegate.newJob(bundle, scheduler);
        }

        return new QuartzWorkflowJob(strategy, workflowService, w.get(), trx, new WorkflowStateParserComponent(mapper));
    }
}
