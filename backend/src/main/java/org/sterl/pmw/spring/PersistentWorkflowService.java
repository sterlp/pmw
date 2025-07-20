package org.sterl.pmw.spring;

import java.io.Serializable;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.sterl.pmw.AbstractWorkflowService;
import org.sterl.pmw.command.TriggerWorkflowCommand;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.RunningWorkflowId;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowStep;
import org.sterl.pmw.spring.component.WorkflowHelper;
import org.sterl.pmw.spring.component.WorkflowStepComponent;
import org.sterl.spring.persistent_tasks.PersistentTaskService;
import org.sterl.spring.persistent_tasks.api.TaskId;
import org.sterl.spring.persistent_tasks.api.TriggerSearch;
import org.sterl.spring.persistent_tasks.api.TriggerStatus;
import org.sterl.spring.persistent_tasks.task.TaskService;
import org.sterl.spring.persistent_tasks.trigger.TriggerService;
import org.sterl.spring.persistent_tasks.trigger.model.RunningTriggerEntity;

import lombok.extern.slf4j.Slf4j;

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
            WorkflowRepository workflowRepository) {
        super(workflowRepository);
        this.triggerService = triggerService;
        this.taskService = taskService;
        this.persistentTaskService = persistentTaskService;
    }
    
    @EventListener(TriggerWorkflowCommand.class)
    public <T extends Serializable> RunningWorkflowId execute(TriggerWorkflowCommand<T> workflowCommand) {
        return execute(workflowCommand.workflow(), workflowCommand.state(), workflowCommand.delay());
    }

    @Override
    public <T extends Serializable> RunningWorkflowId execute(Workflow<T> workflow, T state, Duration delay) {
        final var task = (TaskId<T>)this.firstTaskRef.get(workflow);
        final var id = RunningWorkflowId.newWorkflowId(workflow);
        final var trigger = task
            .newTrigger(state == null ? workflow.newContext() : state)
            .runAfter(delay)
            .tag(getWorkflowId(workflow).get())
            .correlationId(id.value())
            .build();
        
        log.debug("Starting workflow={} with id={} and first step={}", 
                workflow, id.value(), trigger.key());

        persistentTaskService.runOrQueue(trigger);

        return id;
    }

    @Override
    public TriggerStatus status(RunningWorkflowId runningWorkflowId) {
        var status = persistentTaskService.findLastTriggerByCorrelationId(runningWorkflowId.value());
        if (status.isEmpty()) return null;
        return status.get().getStatus();
    }

    @Override
    public void cancel(RunningWorkflowId runningWorkflowId) {
        var search = TriggerSearch.byCorrelationId(runningWorkflowId.value());
        var running = triggerService.searchTriggers(search, Pageable.ofSize(100))
                .stream()
                .map(RunningTriggerEntity::key)
                .toList();
        triggerService.cancel(running);
    }

    @Override
    public <T extends Serializable> TaskId<T> register(String workflowId, Workflow<T> workflow) throws IllegalStateException {
        TaskId<T> firstWorkflowTask = null;
        this.workflowRepository.registerUnique(workflowId, workflow);

        for (WorkflowStep<T> step : workflow.getSteps()) {
            var stepId = taskService.register(WorkflowHelper.stepName(workflowId, step), 
                    new WorkflowStepComponent<>(this, persistentTaskService, workflowId, workflow, step));
            if (firstWorkflowTask == null) firstWorkflowTask = stepId;
        }
        if (firstWorkflowTask == null) {
            throw new IllegalArgumentException("Workflow[id=" + workflowId + "] " + workflow + " has not steps!");
        }
        this.firstTaskRef.put(workflow, firstWorkflowTask);
        return firstWorkflowTask;
    }

    @Override
    public void clearAllWorkflows() {
        super.clearAllWorkflows();
        this.firstTaskRef.clear();
    }

    @Override
    public Optional<String> getWorkflowId(Workflow<?> workflow) {
        return workflowRepository.getWorkflowId(workflow);
    }
}
