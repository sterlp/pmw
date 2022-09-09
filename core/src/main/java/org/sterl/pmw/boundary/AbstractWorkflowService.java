package org.sterl.pmw.boundary;

import java.time.Duration;

import org.sterl.pmw.component.SerializationUtil;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.WorkflowState;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractWorkflowService<RegistryType> implements WorkflowService<RegistryType> {

    protected final WorkflowRepository workflowRepository;

    @Override
    public WorkflowId execute(String workflowName) {
        return execute(workflowRepository.getWorkflow(workflowName));
    }

    @Override
    public WorkflowId execute(String workflowName, WorkflowState state) {
        return execute(workflowName, state, Duration.ZERO);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public WorkflowId execute(String workflowName, WorkflowState state, Duration delay) {
        final Workflow w = workflowRepository.getWorkflow(workflowName);
        SerializationUtil.verifyStateType(w, state);
        return execute(w, state, delay);
    }

    @Override
    public <T extends WorkflowState>  WorkflowId execute(Workflow<T> w) {
        return execute(w, w.newEmtyContext());
    }

    @Override
    public <T extends WorkflowState>  WorkflowId execute(Workflow<T> w, T c) {
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
}
