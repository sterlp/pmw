package org.sterl.pmw.boundary;

import java.time.Duration;

import org.sterl.pmw.component.SerializationUtil;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.WorkflowState;

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
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public WorkflowId execute(String workflowName, WorkflowState state, Duration delay) {
        final Workflow w = workflowRepository.getWorkflow(workflowName);
        SerializationUtil.verifyStateType(w, state);
        return execute(w, state, delay);
    }

    @Override
    public int workflowCount() {
        return this.workflowRepository.workflowCount();
    }
}
