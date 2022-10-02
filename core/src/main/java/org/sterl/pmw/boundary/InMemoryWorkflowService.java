package org.sterl.pmw.boundary;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.sterl.pmw.component.InMemoryWaitingWorkflowComponent;
import org.sterl.pmw.component.LoggingWorkflowStatusObserver;
import org.sterl.pmw.component.SimpleWorkflowExecutor;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.component.WorkflowStatusObserver;
import org.sterl.pmw.model.InternalWorkflowState;
import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStatus;

public class InMemoryWorkflowService extends AbstractWorkflowService<String> {
    private final ExecutorService stepExecutor;
    private final InMemoryWaitingWorkflowComponent waitingWorkflowComponent;
    private final WorkflowStatusObserver observer;
    private Map<WorkflowId, RunningWorkflowState<?>> runningWorkflows = new ConcurrentHashMap<>();

    public InMemoryWorkflowService() {
        this(new LoggingWorkflowStatusObserver());
    }

    public InMemoryWorkflowService(WorkflowStatusObserver observer) {
        super(new WorkflowRepository());
        this.observer = observer;
        this.waitingWorkflowComponent = new InMemoryWaitingWorkflowComponent(this);
        this.stepExecutor = Executors.newWorkStealingPool();
    }

    @Override
    public <T extends WorkflowState> WorkflowId execute(Workflow<T> w, T state, Duration delay) {
        var workflowId = WorkflowId.newWorkflowId(w);
        RunningWorkflowState<T> runningState = new RunningWorkflowState<>(workflowId, w, state, new InternalWorkflowState(delay));

        runningWorkflows.put(workflowId, runningState);
        runOrQueueNextStep(runningState);

        return workflowId;
    }

    @Override
    public void runOrQueueNextStep(RunningWorkflowState<?> runningWorkflowState) {
        final Duration delay = runningWorkflowState.internalState().consumeDelay();
        if (delay.toMillis() <= 0) {
            stepExecutor.submit(new SimpleWorkflowExecutor<>(runningWorkflowState, this, observer));
        } else {
            observer.workflowSuspended(getClass(), Instant.now().plus(delay), runningWorkflowState);
            waitingWorkflowComponent.addWaitingWorkflow(runningWorkflowState, delay);
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
        workflowRepository.registerUnique(w);
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
