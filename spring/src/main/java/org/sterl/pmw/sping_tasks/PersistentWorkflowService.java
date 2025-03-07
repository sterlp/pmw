package org.sterl.pmw.sping_tasks;

import java.io.Serializable;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

import lombok.NonNull;

@Service
public class PersistentWorkflowService extends AbstractWorkflowService<TaskId<? extends Serializable>> {

    private final PersistentTaskService persistentTaskService;
    private final TaskService taskService;
    
    private final Map<Workflow<?>, TaskId<?>> firstTaskRef = new ConcurrentHashMap<>();

    public PersistentWorkflowService(
            PersistentTaskService persistentTaskService,
            TaskService taskService,
            @NonNull WorkflowRepository workflowRepository) {
        super(workflowRepository);
        this.taskService = taskService;
        this.persistentTaskService = persistentTaskService;
    }

    @Override
    public <T extends Serializable> RunningWorkflowId execute(Workflow<T> workflow, T state, Duration delay) {
        var task = (TaskId<T>)this.firstTaskRef.get(workflow);
        var id = RunningWorkflowId.newWorkflowId(workflow);
        persistentTaskService.runOrQueue(
                task.newTrigger(state)
                    .runAfter(delay)
                    .correlationId(id.value())
                    .build());
        return id;
    }

    @Override
    public TriggerStatus status(RunningWorkflowId workflowId) {
        var states = persistentTaskService.findTriggerByCorrelationId(workflowId.value());
        if (states.isEmpty()) return null;
        return states.get(0).getStatus();
    }

    @Override
    public void cancel(RunningWorkflowId workflowId) {
        //persistentTaskService.cancel(new TriggerKey(null, null))
    }

    @Override
    public <T extends Serializable> TaskId<T> register(Workflow<T> workflow) throws IllegalStateException {
        TaskId<T> firstWorkflowTask = null;
        final var name = workflow.getName();
        for (WorkflowStep step : workflow.getSteps()) {
            var id = taskService.register(name + "::" + step.getName(), 
                    new WorkflowStepComponent<>(this, workflow, step));
            if (firstWorkflowTask == null) firstWorkflowTask = id;
        }
        this.workflowRepository.registerUnique(workflow);
        this.firstTaskRef.put(workflow, firstWorkflowTask);
        return firstWorkflowTask;
    }
    
    @Override
    public void clearAllWorkflows() {
        super.clearAllWorkflows();
        this.firstTaskRef.clear();
    }
}
