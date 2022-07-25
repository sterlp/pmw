package org.sterl.pmw.boundary;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.sterl.pmw.component.SimpleWorkflowStepStrategy;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.AbstractWorkflowContext;
import org.sterl.pmw.model.Workflow;

import lombok.RequiredArgsConstructor;

public class InMemoryWorkflowService implements WorkflowService<String> {
    private ExecutorService stepExecutor;
    private WorkflowRepository workflowRepository = new WorkflowRepository();
    private Map<UUID, Workflow<?>> runningWorkflows = new ConcurrentHashMap<>();
    
    public InMemoryWorkflowService() {
        stepExecutor = Executors.newWorkStealingPool();
    }

    public <T extends AbstractWorkflowContext>  String execute(Workflow<T> w) {
        return execute(w, w.newEmtyContext());
    }
    public <T extends AbstractWorkflowContext>  String execute(Workflow<T> w, T c) {
        var workflowId = UUID.randomUUID();
        runningWorkflows.put(workflowId, w);
        stepExecutor.submit(new StepCallable<T>(w, c, workflowId));
        return workflowId.toString();
    }

    @RequiredArgsConstructor
    private class StepCallable<T extends AbstractWorkflowContext>
        extends SimpleWorkflowStepStrategy
        implements Callable<Void> {
        private final Workflow<T> w;
        private final T c;
        private final UUID workflowId;

        @Override
        public Void call() throws Exception {
            if (this.call(w, c)) {
                stepExecutor.submit(new StepCallable<T>(w, c, workflowId));
            } else {
                runningWorkflows.remove(workflowId);
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
    public <T extends AbstractWorkflowContext> String register(Workflow<T> w) {
        workflowRepository.register(w);
        return w.getName();
    }

    @Override
    public <T extends AbstractWorkflowContext> String execute(String workflowName) {
        return execute(workflowRepository.getWorkflow(workflowName));
    }

    @Override
    public <T extends AbstractWorkflowContext> String execute(String workflowName, T c) {
        return execute((Workflow<T>)workflowRepository.getWorkflow(workflowName), c);
    }
}
