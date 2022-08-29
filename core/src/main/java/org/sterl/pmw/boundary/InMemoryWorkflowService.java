package org.sterl.pmw.boundary;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.sterl.pmw.component.SimpleWorkflowStepStrategy;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.exception.WorkflowException;
import org.sterl.pmw.model.InternalWorkflowState;
import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStatus;

import lombok.RequiredArgsConstructor;

public class InMemoryWorkflowService implements WorkflowService<String> {
    private ExecutorService stepExecutor;
    private WorkflowRepository workflowRepository = new WorkflowRepository();

    private Map<String, Workflow<?>> runningWorkflows = new ConcurrentHashMap<>();
    private Map<String, WaitingWorkflow<?>> waitingWorkflows = new ConcurrentHashMap<>();
    
    record WaitingWorkflow<T extends WorkflowState>(Instant until, RunningWorkflowState<T> runningWorkflowState) {};

    public InMemoryWorkflowService() {
        stepExecutor = Executors.newWorkStealingPool();
        stepExecutor.submit(() -> {
            while(true) {
                final Instant now = Instant.now();
                for (Entry<String, WaitingWorkflow<?>> w : new HashSet<>(waitingWorkflows.entrySet())) {
                    if (now.isAfter(w.getValue().until)) {
                        stepExecutor.submit(new StepCallable<>(w.getValue().runningWorkflowState(), w.getKey()));
                        waitingWorkflows.remove(w.getKey());
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    if (Thread.interrupted()) break;
                }
            }
        });
    }

    public <T extends WorkflowState>  String execute(Workflow<T> w) {
        return execute(w, w.newEmtyContext());
    }
    public <T extends WorkflowState>  String execute(Workflow<T> w, T c) {
        var workflowId = UUID.randomUUID().toString();
        runningWorkflows.put(workflowId, w);
        RunningWorkflowState<T> state = new RunningWorkflowState<>(w, c, new InternalWorkflowState());
        stepExecutor.submit(new StepCallable<>(state, workflowId));
        return workflowId.toString();
    }

    @RequiredArgsConstructor
    private class StepCallable<T extends WorkflowState> extends SimpleWorkflowStepStrategy
        implements Callable<Void> {
        private final RunningWorkflowState<T> runningWorkflowState;
        private final String workflowId;

        @Override
        public Void call() throws Exception {
            try {
                if (this.call(runningWorkflowState)) {
                    final Optional<Duration> delay = runningWorkflowState.internalState().clearDelay();
                    if (delay.isEmpty()) {
                        stepExecutor.submit(new StepCallable<>(runningWorkflowState, workflowId));
                    } else {
                        waitingWorkflows.put(workflowId, 
                                new WaitingWorkflow<>(Instant.now().plus(delay.get()), runningWorkflowState));
                    }
                } else {
                    runningWorkflows.remove(workflowId);
                }
            } catch (WorkflowException.WorkflowFailedDoRetryException e) {
                stepExecutor.submit(new StepCallable<>(runningWorkflowState, workflowId));
            }
            return null;
        } 
    }
    
    public void stop() {
        stepExecutor.shutdown();
    }

    @Override
    public void clearAllWorkflows() {
        stepExecutor.shutdownNow();
        runningWorkflows.clear();
        waitingWorkflows.clear();
        stepExecutor = Executors.newWorkStealingPool();
    }

    @Override
    public <T extends WorkflowState> String register(Workflow<T> w) {
        workflowRepository.register(w);
        return w.getName();
    }

    @Override
    public String execute(String workflowName) {
        return execute(workflowRepository.getWorkflow(workflowName));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public String execute(String workflowName, WorkflowState c) {
        Workflow w = (Workflow)workflowRepository.getWorkflow(workflowName);
        final Class<? extends WorkflowState> newContextClass = w.newEmtyContext().getClass();
        if (c != null && newContextClass.isAssignableFrom(c.getClass())) {
            return execute((Workflow)workflowRepository.getWorkflow(workflowName), c);
        } else {
            throw new IllegalArgumentException("Context of type " 
                    + c == null ? "null" : c.getClass().getName()
                    + " is not compatible to " + newContextClass.getName());
        }
    }

    @Override
    public WorkflowStatus status(String workflowId) {
        WorkflowStatus result;
        if (this.waitingWorkflows.containsKey(workflowId)) {
            result = WorkflowStatus.SLEEPING;
        } else if (this.runningWorkflows.containsKey(workflowId)) {
            result = WorkflowStatus.RUNNING;
        } else {
            result = WorkflowStatus.COMPLETE;
        }
        return result;
    }
}
