package org.sterl.pmw.sping_tasks;

import java.io.Serializable;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.sterl.pmw.AbstractWorkflowService;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.RunningWorkflowId;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowStep;
import org.sterl.pmw.sping_tasks.component.WorkflowStepComponent;
import org.sterl.spring.persistent_tasks.PersistentTaskService;
import org.sterl.spring.persistent_tasks.api.TaskId;
import org.sterl.spring.persistent_tasks.api.TriggerStatus;
import org.sterl.spring.persistent_tasks.task.TaskService;
import org.sterl.spring.persistent_tasks.trigger.TriggerService;
import org.sterl.spring.persistent_tasks.trigger.model.TriggerEntity;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PersistentWorkflowService extends AbstractWorkflowService<TaskId<? extends Serializable>> {

    private final PersistentTaskService persistentTaskService;
    private final TriggerService triggerService;
    private final TaskService taskService;

    private final Map<Workflow<?>, TaskId<?>> firstTaskRef = new ConcurrentHashMap<>();

    public PersistentWorkflowService(
            PersistentTaskService persistentTaskService,
            TriggerService triggerService,
            TaskService taskService,
            @NonNull WorkflowRepository workflowRepository) {
        super(workflowRepository);
        this.triggerService = triggerService;
        this.taskService = taskService;
        this.persistentTaskService = persistentTaskService;
    }

    @Override
    public <T extends Serializable> RunningWorkflowId execute(Workflow<T> workflow, T state, Duration delay) {
        final var task = (TaskId<T>)this.firstTaskRef.get(workflow);
        final var id = RunningWorkflowId.newWorkflowId(workflow);
        final var trigger = task.newTrigger(state)
            .runAfter(delay)
            .correlationId(id.value())
            .build();
        
        log.debug("Starting workflow={} with id={} and first step={}", 
                workflow.getName(), id.value(), trigger.key());

        persistentTaskService.runOrQueue(trigger);

        return id;
    }

    @Override
    public TriggerStatus status(RunningWorkflowId workflowId) {
        // TODO just load one and not all!!!
        var status = persistentTaskService.findLastTriggerByCorrelationId(workflowId.value());
        if (status.isEmpty()) return TriggerStatus.SUCCESS;
        return status.get().getStatus();
    }

    @Override
    public void cancel(RunningWorkflowId workflowId) {
        var running = triggerService.findTriggerByCorrelationId(workflowId.value(), Pageable.ofSize(100))
                .stream()
                .filter(TriggerEntity::isWaiting)
                .map(TriggerEntity::key)
                .toList();
        triggerService.cancel(running);
    }

    @Override
    public <T extends Serializable> TaskId<T> register(Workflow<T> workflow) throws IllegalStateException {
        TaskId<T> firstWorkflowTask = null;
        this.workflowRepository.registerUnique(workflow);
        final var name = workflow.getName();
        for (WorkflowStep step : workflow.getSteps()) {
            var id = taskService.register(name + "::" + step.getName(), 
                    new WorkflowStepComponent<>(this, persistentTaskService, workflow, step));
            if (firstWorkflowTask == null) firstWorkflowTask = id;
        }
        if (firstWorkflowTask == null) throw new IllegalArgumentException("Workflow " + workflow.getName() + " has not steps!");
        this.firstTaskRef.put(workflow, firstWorkflowTask);
        return firstWorkflowTask;
    }

    @Override
    public void clearAllWorkflows() {
        super.clearAllWorkflows();
        this.firstTaskRef.clear();
    }
}
