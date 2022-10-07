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
import org.sterl.pmw.component.SimpleWorkflowStepExecutor;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.component.WorkflowStatusObserver;
import org.sterl.pmw.model.InternalWorkflowState;
import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStatus;

public class InMemoryWorkflowService extends AbstractWorkflowService<String> {
    private final ExecutorService stepExecutorPool;
    private final InMemoryWaitingWorkflowComponent waitingWorkflowComponent;
    private final WorkflowStatusObserver observer;
    private final SimpleWorkflowStepExecutor stepExecutor;
    private Map<WorkflowId, RunningWorkflowState<?>> runningWorkflows = new ConcurrentHashMap<>();

    public InMemoryWorkflowService() {
        this(new LoggingWorkflowStatusObserver());
    }

    public InMemoryWorkflowService(WorkflowStatusObserver observer) {
        super(new WorkflowRepository());
        this.observer = observer;
        this.waitingWorkflowComponent = new InMemoryWaitingWorkflowComponent(this);
        this.stepExecutorPool = Executors.newWorkStealingPool();
        this.stepExecutor = new SimpleWorkflowStepExecutor(observer);
    }

    @Override
    public <T extends WorkflowState> WorkflowId execute(Workflow<T> w, T state, Duration delay) {
        var workflowId = WorkflowId.newWorkflowId(w);
        RunningWorkflowState<T> runningState = new RunningWorkflowState<>(workflowId, w, state, new InternalWorkflowState(delay));

        runningWorkflows.put(workflowId, runningState);
        observer.workdlowCreated(getClass(), workflowId, w, state);
        
        queueStepForExecution(runningState);

        return workflowId;
    }

    @Override
    public void queueStepForExecution(RunningWorkflowState<?> runningWorkflowState) {
        final Duration delay = runningWorkflowState.internalState().consumeDelay();
        if (delay.toMillis() <= 0) {
            stepExecutorPool.submit(new SimpleWorkflowExecutor<>(runningWorkflowState, this, observer, stepExecutor));
        } else {
            observer.workflowSuspended(getClass(), Instant.now().plus(delay), runningWorkflowState);
            waitingWorkflowComponent.addWaitingWorkflow(runningWorkflowState, delay);
        }
    }

    public void stop() throws InterruptedException {
        stepExecutorPool.awaitTermination(30, TimeUnit.SECONDS);
        stepExecutorPool.shutdown();
    }

    @Override
    public void clearAllWorkflows() {
        super.clearAllWorkflows();
        this.waitingWorkflowComponent.clear();
        this.runningWorkflows.clear();
    }

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

    @Override
    public void fail(WorkflowId workflowId) {
        this.waitingWorkflowComponent.remove(workflowId);
        this.runningWorkflows.remove(workflowId);
    }
}
