package org.sterl.pmw.boundary;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.sterl.pmw.component.InMemoryWaitingWorkflowComponent;
import org.sterl.pmw.component.SimpleWorkflowExecutor;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.InternalWorkflowState;
import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InMemoryWorkflowService extends AbstractWorkflowService<String> {
    private final ExecutorService stepExecutor;
    private final InMemoryWaitingWorkflowComponent waitingWorkflowComponent;

    private Map<WorkflowId, RunningWorkflowState<?>> runningWorkflows = new ConcurrentHashMap<>();


    public InMemoryWorkflowService() {
        super(new WorkflowRepository());
        this.waitingWorkflowComponent = new InMemoryWaitingWorkflowComponent(this);
        this.stepExecutor = Executors.newWorkStealingPool();
    }

    @Override
    public <T extends WorkflowState> WorkflowId execute(Workflow<T> w, T state, Duration delay) {
        var workflowId = WorkflowId.newWorkflowId(w);
        RunningWorkflowState<T> runningState = new RunningWorkflowState<>(w, state, new InternalWorkflowState(delay));

        runningWorkflows.put(workflowId, runningState);
        runOrQueueNextStep(workflowId, runningState);

        return workflowId;
    }

    @Override
    public void runOrQueueNextStep(WorkflowId workflowId, RunningWorkflowState<?> runningWorkflowState) {
        final Duration delay = runningWorkflowState.internalState().consumeDelay();
        if (delay.toMillis() <= 0) {
            stepExecutor.submit(new SimpleWorkflowExecutor<>(workflowId, runningWorkflowState, this));
            log.debug("Started workflow={} with id={}", runningWorkflowState.workflow().getName(), workflowId);
        } else {
            waitingWorkflowComponent.addWaitingWorkflow(workflowId, runningWorkflowState, delay);
        }
    }

    public void stop() throws InterruptedException {
        stepExecutor.awaitTermination(30, TimeUnit.SECONDS);
        stepExecutor.shutdown();
    }

    @Override
    public void clearAllWorkflows() {
        super.clearAllWorkflows();
        this.waitingWorkflowComponent.clear();
        this.runningWorkflows.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends WorkflowState> String register(Workflow<T> w) {
        workflowRepository.registerUnique((Workflow<WorkflowState>)w);
        return w.getName();
    }

    @Override
    public WorkflowStatus status(WorkflowId workflowId) {
        WorkflowStatus result;
        if (this.waitingWorkflowComponent.isWaiting(workflowId)) {
            result = WorkflowStatus.SLEEPING;
        } else {
            final RunningWorkflowState<?> running = this.runningWorkflows.get(workflowId);
            if (running == null) {
                result = WorkflowStatus.COMPLETE;
            } else {
                result = running.internalState().getWorkflowStatus();
            }
        }
        return result;
    }

    @Override
    public void cancel(WorkflowId workflowId) {
        this.waitingWorkflowComponent.remove(workflowId);
        this.runningWorkflows.remove(workflowId);
    }
}
