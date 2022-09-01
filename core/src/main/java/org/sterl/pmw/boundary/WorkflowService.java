package org.sterl.pmw.boundary;

import java.time.Duration;

import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStatus;

public interface WorkflowService<RegistryType> {
    String execute(String workflowName);
    String execute(String workflowName, WorkflowState state);
    String execute(String workflowName, WorkflowState state, Duration delay);

    <T extends WorkflowState> String execute(Workflow<T> w);
    <T extends WorkflowState> String execute(Workflow<T> w, T state);
    <T extends WorkflowState> String execute(Workflow<T> w, T state, Duration delay);

    WorkflowStatus status(String workflowId);

    void clearAllWorkflows();

    /**
     * Should register a new workflow or throw an exception if the workflow is already known.
     * 
     * @throws IllegalArgumentException if the workflow is already known
     */
    <T extends WorkflowState> RegistryType register(Workflow<T> w) throws IllegalArgumentException;
    
    int workflowCount();
}
