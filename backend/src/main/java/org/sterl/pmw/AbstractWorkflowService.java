package org.sterl.pmw;

import java.io.Serializable;
import java.time.Duration;
import java.util.Collection;

import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.Workflow;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractWorkflowService<RegistryType> implements WorkflowService<RegistryType> {

    @NonNull
    protected final WorkflowRepository workflowRepository;

    @Override
    public WorkflowId execute(String workflowName) {
        return execute(workflowRepository.getWorkflow(workflowName));
    }

    @Override
    public WorkflowId execute(String workflowName, Serializable state) {
        return execute(workflowName, state, Duration.ZERO);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public WorkflowId execute(String workflowName, Serializable state, Duration delay) {
        final Workflow w = workflowRepository.getWorkflow(workflowName);
        return execute(w, state, delay);
    }

    @Override
    public <T extends Serializable>  WorkflowId execute(Workflow<T> w) {
        return execute(w, w.newContext());
    }

    @Override
    public <T extends Serializable>  WorkflowId execute(Workflow<T> w, T c) {
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
    public Collection<String> listWorkflows() {
        return workflowRepository.getWorkflowNames();
    }
}
