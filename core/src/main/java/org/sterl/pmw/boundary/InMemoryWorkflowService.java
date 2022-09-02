package org.sterl.pmw.boundary;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.sterl.pmw.component.SerializationUtil;
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

    private record WaitingWorkflow<T extends WorkflowState>(Instant until, RunningWorkflowState<T> runningWorkflowState) {}

    @RequiredArgsConstructor
    private class StepCallable<T extends WorkflowState> extends SimpleWorkflowStepStrategy
        implements Callable<Void> {
        private final RunningWorkflowState<T> runningWorkflowState;
        private final String workflowId;

        @Override
        public Void call() throws Exception {

            byte[] originalState = SerializationUtil.serialize(runningWorkflowState.userState());

            try {
                if (this.call(runningWorkflowState)) {
                    queueNextStepExecution(workflowId, runningWorkflowState);
                } else {
                    runningWorkflows.remove(workflowId);
                }
            } catch (WorkflowException.WorkflowFailedDoRetryException e) {

                queueNextStepExecution(workflowId, new RunningWorkflowState<>(
                        runningWorkflowState.workflow(),
                        SerializationUtil.deserializeWorkflowState(originalState),
                        runningWorkflowState.internalState())
                    );
            }
            return null;
        }
    }

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

    @Override
    public <T extends WorkflowState>  String execute(Workflow<T> w) {
        return execute(w, w.newEmtyContext());
    }

    @Override
    public <T extends WorkflowState>  String execute(Workflow<T> w, T c) {
        return execute(w, c, Duration.ZERO);
    }
    
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public String execute(String workflowName, WorkflowState state, Duration delay) {
        final Workflow w = workflowRepository.getWorkflow(workflowName);
        SerializationUtil.verifyStateType(w, state);
        return execute(w, state, delay);
    }

    @Override
    public <T extends WorkflowState> String execute(Workflow<T> w, T state, Duration delay) {
        var workflowId = UUID.randomUUID().toString();
        runningWorkflows.put(workflowId, w);
        RunningWorkflowState<T> runningState = new RunningWorkflowState<>(w, state, new InternalWorkflowState(delay));
        
        queueNextStepExecution(workflowId, runningState);
        
        return workflowId;
    }

    private void queueNextStepExecution(String workflowId, RunningWorkflowState<?> runningWorkflowState) {
        final Duration delay = runningWorkflowState.internalState().consumeDelay();
        if (delay.toMillis() <= 0) {
            stepExecutor.submit(new StepCallable<>(runningWorkflowState, workflowId));
        } else {
            waitingWorkflows.put(workflowId,
                    new WaitingWorkflow<>(Instant.now().plus(delay), runningWorkflowState));
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

    @SuppressWarnings("unchecked")
    @Override
    public <T extends WorkflowState> String register(Workflow<T> w) {
        workflowRepository.registerUnique((Workflow<WorkflowState>)w);
        return w.getName();
    }

    @Override
    public String execute(String workflowName) {
        return execute(workflowRepository.getWorkflow(workflowName));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public String execute(String workflowName, WorkflowState c) {
        final Workflow w = workflowRepository.getWorkflow(workflowName);
        SerializationUtil.verifyStateType(w, c);
        return execute(w, c);
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

    @Override
    public int workflowCount() {
        return this.workflowRepository.workflowCount();
    }
}
