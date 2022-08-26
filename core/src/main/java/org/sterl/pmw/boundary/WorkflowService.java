package org.sterl.pmw.boundary;

import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStatus;

public interface WorkflowService<RegistryType> {
    String execute(String workflowName);
    String execute(String workflowName, WorkflowState c);
    <T extends WorkflowState> String execute(Workflow<T> w);
    <T extends WorkflowState> String execute(Workflow<T> w, T c);
    WorkflowStatus status(String workflowId);
    void clearAllWorkflows();
    <T extends WorkflowState> RegistryType register(Workflow<T> w);
}
