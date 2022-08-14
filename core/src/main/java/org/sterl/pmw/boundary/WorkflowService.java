package org.sterl.pmw.boundary;

import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowContext;

public interface WorkflowService<RegistryType> {
    enum WorkflowStatus {
        PENDING,
        RUNNING,
        COMPLETE
    }
    
    String execute(String workflowName);
    String execute(String workflowName, WorkflowContext c);
    <T extends WorkflowContext> String execute(Workflow<T> w);
    <T extends WorkflowContext> String execute(Workflow<T> w, T c);
    WorkflowStatus status(String workflowId);
    void clearAllWorkflows();
    <T extends WorkflowContext> RegistryType register(Workflow<T> w);
}
