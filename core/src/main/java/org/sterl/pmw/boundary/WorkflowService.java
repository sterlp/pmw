package org.sterl.pmw.boundary;

import org.sterl.pmw.model.AbstractWorkflowContext;
import org.sterl.pmw.model.Workflow;

public interface WorkflowService<RegistryType> {
    enum WorkflowStatus {
        PENDING,
        RUNNING,
        COMPLETE
    }
    
    <T extends AbstractWorkflowContext> String execute(String workflowName);
    <T extends AbstractWorkflowContext> String execute(String workflowName, T c);
    <T extends AbstractWorkflowContext> String execute(Workflow<T> w);
    <T extends AbstractWorkflowContext> String execute(Workflow<T> w, T c);
    WorkflowStatus status(String workflowId);
    void clearAllWorkflows();
    <T extends AbstractWorkflowContext> RegistryType register(Workflow<T> w);
}
