package org.sterl.pmw.boundary;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.sterl.pmw.component.SimpleWorkflowStepStrategy;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.exception.WorkflowException;
import org.sterl.pmw.model.InternalWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowContext;
import org.sterl.pmw.model.WorkflowState;

import lombok.RequiredArgsConstructor;

public class InMemoryWorkflowService implements WorkflowService<String> {
    private ExecutorService stepExecutor;
    private WorkflowRepository workflowRepository = new WorkflowRepository();
    private Map<UUID, Workflow<?>> runningWorkflows = new ConcurrentHashMap<>();
    
    public InMemoryWorkflowService() {
        stepExecutor = Executors.newWorkStealingPool();
    }

    public <T extends WorkflowContext>  String execute(Workflow<T> w) {
        return execute(w, w.newEmtyContext());
    }
    public <T extends WorkflowContext>  String execute(Workflow<T> w, T c) {
        var workflowId = UUID.randomUUID();
        runningWorkflows.put(workflowId, w);
        WorkflowState state = new WorkflowState(w, c, new InternalWorkflowState());
        stepExecutor.submit(new StepCallable(state, workflowId));
        return workflowId.toString();
    }

    @RequiredArgsConstructor
    private class StepCallable extends SimpleWorkflowStepStrategy
        implements Callable<Void> {
        private final WorkflowState workflowState;
        private final UUID workflowId;

        @Override
        public Void call() throws Exception {
            try {
                if (this.call(workflowState)) {
                    stepExecutor.submit(new StepCallable(workflowState, workflowId));
                } else {
                    runningWorkflows.remove(workflowId);
                }
            } catch (WorkflowException.WorkflowFailedDoRetryException e) {
                stepExecutor.submit(new StepCallable(workflowState, workflowId));
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
        stepExecutor = Executors.newWorkStealingPool();
    }

    @Override
    public <T extends WorkflowContext> String register(Workflow<T> w) {
        workflowRepository.register(w);
        return w.getName();
    }

    @Override
    public String execute(String workflowName) {
        return execute(workflowRepository.getWorkflow(workflowName));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public String execute(String workflowName, WorkflowContext c) {
        Workflow w = (Workflow)workflowRepository.getWorkflow(workflowName);
        final Class<? extends WorkflowContext> newContextClass = w.newEmtyContext().getClass();
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
        return this.runningWorkflows.containsKey(UUID.fromString(workflowId)) ? WorkflowStatus.RUNNING : WorkflowStatus.COMPLETE;
    }
}
