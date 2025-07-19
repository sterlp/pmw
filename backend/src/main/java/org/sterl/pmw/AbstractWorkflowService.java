package org.sterl.pmw;

import java.io.Serializable;
import java.time.Duration;
import java.util.Collection;

import org.sterl.pmw.api.WorkflowInfo;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.RunningWorkflowId;
import org.sterl.pmw.model.Workflow;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractWorkflowService<RegistryType> implements WorkflowService<RegistryType> {

    @NonNull
    protected final WorkflowRepository workflowRepository;

    @Override
    public RunningWorkflowId execute(String workflowId) {
        return execute(workflowRepository.getWorkflow(workflowId));
    }

    @Override
    public RunningWorkflowId execute(String workflowId, Serializable state) {
        return execute(workflowId, state, Duration.ZERO);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public RunningWorkflowId execute(String workflowId, Serializable state, Duration delay) {
        final Workflow w = workflowRepository.getWorkflow(workflowId);
        return execute(w, state, delay);
    }

    @Override
    public <T extends Serializable> RunningWorkflowId execute(Workflow<T> w) {
        return execute(w, w.newContext());
    }

    @Override
    public <T extends Serializable> RunningWorkflowId execute(Workflow<T> w, T c) {
        return execute(w, c, Duration.ZERO);
    }

    @Override
    public void clearAllWorkflows() {
        this.workflowRepository.clear();
    }

    @Override
    public int workflowCount() {
        return this.workflowRepository.workflowCount();
    }

    @Override
    public Collection<WorkflowInfo> listWorkflows() {
        return workflowRepository.getWorkflows().entrySet()
            .stream()
            .map(e -> new WorkflowInfo(e.getKey(), 
                        e.getValue().getName(),
                        e.getValue().getStepCount()))
            .toList();
    }
}
